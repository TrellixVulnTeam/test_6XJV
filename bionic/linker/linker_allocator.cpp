/*
 * Copyright (C) 2015 The Android Open Source Project
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *  * Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *  * Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS
 * OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED
 * AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 */

#include "linker_allocator.h"
#include "linker_debug.h"
#include "linker.h"

#include <stdlib.h>
#include <sys/mman.h>
#include <unistd.h>

#include <async_safe/log.h>

#include "private/bionic_prctl.h"

//
// LinkerMemeoryAllocator is general purpose allocator
// designed to provide the same functionality as the malloc/free/realloc
// libc functions.
//
// On alloc:
// If size is >= 1k allocator proxies malloc call directly to mmap
// If size < 1k allocator uses SmallObjectAllocator for the size
// rounded up to the nearest power of two.
//
// On free:
//
// For a pointer allocated using proxy-to-mmap allocator unmaps
// the memory.
//
// For a pointer allocated using SmallObjectAllocator it adds
// the block to free_blocks_list in the corresponding page. If the number of
// free pages reaches 2, SmallObjectAllocator munmaps one of the pages keeping
// the other one in reserve.

// Memory management for large objects is fairly straightforward, but for small
// objects it is more complicated.  If you are changing this code, one simple
// way to evaluate the memory usage change is by running 'dd' and examine the
// memory usage by 'showmap $(pidof dd)'.  'dd' is nice in that:
//   1. It links in quite a few libraries, so you get some linker memory use.
//   2. When run with no arguments, it sits waiting for input, so it is easy to
//      examine its memory usage with showmap.
//   3. Since it does nothing while waiting for input, the memory usage is
//      determinisitic.

static const char kSignature[4] = {'L', 'M', 'A', 1};

static const size_t kSmallObjectMaxSize = 1 << kSmallObjectMaxSizeLog2;

// This type is used for large allocations (with size >1k)
static const uint32_t kLargeObject = 111;

// Allocated pointers must be at least 16-byte aligned.  Round up the size of
// page_info to multiple of 16.
static constexpr size_t kPageInfoSize = __BIONIC_ALIGN(sizeof(page_info), 16);

static inline uint16_t log2(size_t number) {
  uint16_t result = 0;
  number--;

  while (number != 0) {
    result++;
    number >>= 1;
  }

  return result;
}

LinkerSmallObjectAllocator::LinkerSmallObjectAllocator(uint32_t type,
                                                       size_t block_size)
    : type_(type),
      block_size_(block_size),
      blocks_per_page_((PAGE_SIZE - sizeof(small_object_page_info)) /
	               block_size),
      free_pages_cnt_(0),
      page_list_(nullptr) {}

void* LinkerSmallObjectAllocator::alloc() {
  CHECK(block_size_ != 0);

  if (page_list_ == nullptr) {
    alloc_page();
  }

  // Fully allocated pages are de-managed and removed from the page list, so
  // every page from the page list must be useable.  Let's just take the first
  // one.
  small_object_page_info* page = page_list_;
  CHECK(page->free_block_list != nullptr);

  small_object_block_record* const block_record = page->free_block_list;
  if (block_record->free_blocks_cnt > 1) {
    small_object_block_record* next_free =
        reinterpret_cast<small_object_block_record*>(
            reinterpret_cast<uint8_t*>(block_record) + block_size_);
    next_free->next = block_record->next;
    next_free->free_blocks_cnt = block_record->free_blocks_cnt - 1;
    page->free_block_list = next_free;
  } else {
    page->free_block_list = block_record->next;
  }

  if (page->free_blocks_cnt == blocks_per_page_) {
    free_pages_cnt_--;
  }

  page->free_blocks_cnt--;

  memset(block_record, 0, block_size_);

  if (page->free_blocks_cnt == 0) {
    // De-manage fully allocated pages.  These pages will be managed again if
    // a block is freed.
    remove_from_page_list(page);
  }

  return block_record;
}

void LinkerSmallObjectAllocator::free_page(small_object_page_info* page) {
  CHECK(page->free_blocks_cnt == blocks_per_page_);
  if (page->prev_page) {
    page->prev_page->next_page = page->next_page;
  }
  if (page->next_page) {
    page->next_page->prev_page = page->prev_page;
  }
  if (page_list_ == page) {
    page_list_ = page->next_page;
  }
  munmap(page, PAGE_SIZE);
  free_pages_cnt_--;
}

void LinkerSmallObjectAllocator::free(void* ptr) {
  small_object_page_info* const page =
      reinterpret_cast<small_object_page_info*>(
          PAGE_START(reinterpret_cast<uintptr_t>(ptr)));

  if (reinterpret_cast<uintptr_t>(ptr) % block_size_ != 0) {
    async_safe_fatal("invalid pointer: %p (block_size=%zd)", ptr, block_size_);
  }

  memset(ptr, 0, block_size_);
  small_object_block_record* const block_record =
      reinterpret_cast<small_object_block_record*>(ptr);

  block_record->next = page->free_block_list;
  block_record->free_blocks_cnt = 1;

  page->free_block_list = block_record;
  page->free_blocks_cnt++;

  if (page->free_blocks_cnt == blocks_per_page_) {
    if (++free_pages_cnt_ > 1) {
      // if we already have a free page - unmap this one.
      free_page(page);
    }
  } else if (page->free_blocks_cnt == 1) {
    // We just freed from a full page.  Add this page back to the list.
    add_to_page_list(page);
  }
}

void LinkerSmallObjectAllocator::alloc_page() {
  void* const map_ptr = mmap(nullptr, PAGE_SIZE, PROT_READ | PROT_WRITE,
                             MAP_PRIVATE | MAP_ANONYMOUS, -1, 0);
  if (map_ptr == MAP_FAILED) {
    async_safe_fatal("mmap failed: %s", strerror(errno));
  }

  prctl(PR_SET_VMA, PR_SET_VMA_ANON_NAME, map_ptr, PAGE_SIZE,
        "linker_alloc_small_objects");

  small_object_page_info* const page =
      reinterpret_cast<small_object_page_info*>(map_ptr);
  memcpy(page->info.signature, kSignature, sizeof(kSignature));
  page->info.type = type_;
  page->info.allocator_addr = this;

    page->free_blocks_cnt = blocks_per_page_;
  // Align the first block to block_size_.
  const uintptr_t first_block_addr =
      __BIONIC_ALIGN(reinterpret_cast<uintptr_t>(page + 1), block_size_);

    small_object_block_record* const first_block =
        reinterpret_cast<small_object_block_record*>(first_block_addr);

  first_block->next = nullptr;
  first_block->free_blocks_cnt = blocks_per_page_;

  page->free_block_list = first_block;

  add_to_page_list(page);

  free_pages_cnt_++;
}

void LinkerSmallObjectAllocator::add_to_page_list(small_object_page_info* page) {
  page->next_page = page_list_;
  page->prev_page = nullptr;
  if (page_list_) {
    page_list_->prev_page = page;
  }
  page_list_ = page;
}

void LinkerSmallObjectAllocator::remove_from_page_list(
    small_object_page_info* page) {
  if (page->prev_page) {
    page->prev_page->next_page = page->next_page;
  }
  if (page->next_page) {
    page->next_page->prev_page = page->prev_page;
  }
  if (page_list_ == page) {
    page_list_ = page->next_page;
  }
  page->prev_page = nullptr;
  page->next_page = nullptr;
}

void LinkerMemoryAllocator::initialize_allocators() {
  if (allocators_ != nullptr) {
    return;
  }

  LinkerSmallObjectAllocator* allocators =
      reinterpret_cast<LinkerSmallObjectAllocator*>(allocators_buf_);

  for (size_t i = 0; i < kSmallObjectAllocatorsCount; ++i) {
    uint32_t type = i + kSmallObjectMinSizeLog2;
    new (allocators + i) LinkerSmallObjectAllocator(type, 1 << type);
  }

  allocators_ = allocators;
}

void* LinkerMemoryAllocator::alloc_mmap(size_t size) {
  size_t allocated_size = PAGE_END(size + kPageInfoSize);
  void* map_ptr = mmap(nullptr, allocated_size, PROT_READ|PROT_WRITE, MAP_PRIVATE|MAP_ANONYMOUS,
                       -1, 0);

  if (map_ptr == MAP_FAILED) {
    async_safe_fatal("mmap failed: %s", strerror(errno));
  }

  prctl(PR_SET_VMA, PR_SET_VMA_ANON_NAME, map_ptr, allocated_size, "linker_alloc_lob");

  page_info* info = reinterpret_cast<page_info*>(map_ptr);
  memcpy(info->signature, kSignature, sizeof(kSignature));
  info->type = kLargeObject;
  info->allocated_size = allocated_size;

  return reinterpret_cast<void*>(reinterpret_cast<uintptr_t>(info) +
                                 kPageInfoSize);
}

void* LinkerMemoryAllocator::alloc(size_t size) {
  // treat alloc(0) as alloc(1)
  if (size == 0) {
    size = 1;
  }

  if (size > kSmallObjectMaxSize) {
    return alloc_mmap(size);
  }

  uint16_t log2_size = log2(size);

  if (log2_size < kSmallObjectMinSizeLog2) {
    log2_size = kSmallObjectMinSizeLog2;
  }

  return get_small_object_allocator(log2_size)->alloc();
}

page_info* LinkerMemoryAllocator::get_page_info(void* ptr) {
  page_info* info = reinterpret_cast<page_info*>(PAGE_START(reinterpret_cast<size_t>(ptr)));
  if (memcmp(info->signature, kSignature, sizeof(kSignature)) != 0) {
    async_safe_fatal("invalid pointer %p (page signature mismatch)", ptr);
  }

  return info;
}

void* LinkerMemoryAllocator::realloc(void* ptr, size_t size) {
  if (ptr == nullptr) {
    return alloc(size);
  }

  if (size == 0) {
    free(ptr);
    return nullptr;
  }

  page_info* info = get_page_info(ptr);

  size_t old_size = 0;

  if (info->type == kLargeObject) {
    old_size = info->allocated_size - kPageInfoSize;
  } else {
    LinkerSmallObjectAllocator* allocator = get_small_object_allocator(info->type);
    if (allocator != info->allocator_addr) {
      async_safe_fatal("invalid pointer %p (page signature mismatch)", ptr);
    }

    old_size = allocator->get_block_size();
  }

  if (old_size < size) {
    void *result = alloc(size);
    memcpy(result, ptr, old_size);
    free(ptr);
    return result;
  }

  return ptr;
}

void LinkerMemoryAllocator::free(void* ptr) {
  if (ptr == nullptr) {
    return;
  }

  page_info* info = get_page_info(ptr);

  if (info->type == kLargeObject) {
    munmap(info, info->allocated_size);
  } else {
    LinkerSmallObjectAllocator* allocator = get_small_object_allocator(info->type);
    if (allocator != info->allocator_addr) {
      async_safe_fatal("invalid pointer %p (invalid allocator address for the page)", ptr);
    }

    allocator->free(ptr);
  }
}

LinkerSmallObjectAllocator* LinkerMemoryAllocator::get_small_object_allocator(uint32_t type) {
  if (type < kSmallObjectMinSizeLog2 || type > kSmallObjectMaxSizeLog2) {
    async_safe_fatal("invalid type: %u", type);
  }

  initialize_allocators();
  return &allocators_[type - kSmallObjectMinSizeLog2];
}
