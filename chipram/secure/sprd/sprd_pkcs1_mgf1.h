#ifndef SPRD_PKCS1_MGF1_H
#define SPRD_PKCS1_MGF1_H
#include <security/sprd_crypto_r3p0.h>
uint32_t sprd_pkcs1_mgf1(sprd_crypto_algo_t hash_type, const unsigned char *seed, unsigned long seedlen,
                      unsigned char *mask, unsigned long masklen);

#endif
