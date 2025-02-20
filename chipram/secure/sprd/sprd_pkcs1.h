#ifndef SPRD_PKCS1_H
#define SPRD_PKCS1_H

#include <security/sprd_crypto_r3p0.h>

enum sprd_pkcs_1_v1_5_padding_type
{
	SPRD_PKCS_1_EMSA   = 1,        /* Block type 1 (PKCS #1 v1.5 signature padding) */
	SPRD_PKCS_1_EME    = 2         /* Block type 2 (PKCS #1 v1.5 encryption padding) */
};

uint32_t sprd_pkcs1_v1_5_decode(const uint8_t *msg,
		uint32_t msglen,
		int32_t block_type,
		uint32_t modulus_len,
		uint8_t *out,
		uint32_t *outlen,
		int32_t *is_valid);

uint32_t sprd_pkcs1_pss_decode(const uint8_t *msghash, uint32_t msghashlen,
		const uint8_t *sig, uint32_t siglen,
		uint32_t saltlen, sprd_crypto_algo_t hash_type, sprd_crypto_algo_t mgf1_hash_type,
		uint32_t modulus_bitlen, int *res);
#endif
