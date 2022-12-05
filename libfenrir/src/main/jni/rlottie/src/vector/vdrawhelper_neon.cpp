#if defined(__ARM_NEON__) || defined(__aarch64__)

#include <arm_neon.h>
#include "vdrawhelper.h"

void memfill32(uint32_t *dest, uint32_t value, int count) {
    const int epilogueSize = count % 16;
#if defined(__aarch64__)
    if (count >= 16) {
        uint32_t *const neonEnd = dest + count - epilogueSize;
        register uint32x4_t valueVector1 asm ("v0") = vdupq_n_u32(value);
        register uint32x4_t valueVector2 asm ("v1") = valueVector1;
        while (dest != neonEnd) {
            asm volatile (
            "st2     { v0.4s, v1.4s }, [%[DST]], #32 \n\t"
            "st2     { v0.4s, v1.4s }, [%[DST]], #32 \n\t"
            : [DST]"+r"(dest)
            : [VALUE1]"w"(valueVector1), [VALUE2]"w"(valueVector2)
            : "memory"
            );
        }
    }
#else
    if (count >= 16) {
        uint32_t *const neonEnd = dest + count - epilogueSize;
        register uint32x4_t valueVector1 asm ("q0") = vdupq_n_u32(value);
        register uint32x4_t valueVector2 asm ("q1") = valueVector1;
        while (dest != neonEnd) {
            asm volatile (
            "vst2.32     { d0, d1, d2, d3 }, [%[DST]] !\n\t"
            "vst2.32     { d0, d1, d2, d3 }, [%[DST]] !\n\t"
            : [DST]"+r" (dest)
            : [VALUE1]"w"(valueVector1), [VALUE2]"w"(valueVector2)
            : "memory"
            );
        }
    }
#endif

    switch (epilogueSize) {
        case 15:
            *dest++ = value;
        case 14:
            *dest++ = value;
        case 13:
            *dest++ = value;
        case 12:
            *dest++ = value;
        case 11:
            *dest++ = value;
        case 10:
            *dest++ = value;
        case 9:
            *dest++ = value;
        case 8:
            *dest++ = value;
        case 7:
            *dest++ = value;
        case 6:
            *dest++ = value;
        case 5:
            *dest++ = value;
        case 4:
            *dest++ = value;
        case 3:
            *dest++ = value;
        case 2:
            *dest++ = value;
        case 1:
            *dest = value;
    }
}

#define SIMD_EPILOGUE(i, length, max) \
    for (int _i = 0; _i < (max) && (i) < (length); ++(i), ++_i)

static inline uint16x8_t qvdiv_255_u16(uint16x8_t x, uint16x8_t half) {
    // result = (x + (x >> 8) + 0x80) >> 8

    const uint16x8_t temp = vshrq_n_u16(x, 8); // x >> 8
    const uint16x8_t sum_part = vaddq_u16(x, half); // x + 0x80
    const uint16x8_t sum = vaddq_u16(temp, sum_part);

    return vshrq_n_u16(sum, 8);
}


static inline uint16x8_t qvbyte_mul_u16(uint16x8_t x, uint16x8_t alpha, uint16x8_t half) {
    // t = qRound(x * alpha / 255.0)

    const uint16x8_t t = vmulq_u16(x, alpha); // t
    return qvdiv_255_u16(t, half);
}

static void
comp_func_solid_Source_neon(uint32_t *destPixels, int length, uint32_t color,
                            uint32_t const_alpha) {
    if (const_alpha == 255) {
        memfill32(destPixels, color, length);
    } else {
        const uint32_t minusAlphaOfColor = 255 - const_alpha;
        color = BYTE_MUL(color, const_alpha);
        int x = 0;

        auto *dst = (uint32_t *) destPixels;
        const uint32x4_t colorVector = vdupq_n_u32(color);
        uint16x8_t half = vdupq_n_u16(0x80);
        const uint16x8_t minusAlphaOfColorVector = vdupq_n_u16(minusAlphaOfColor);

        for (; x < length - 3; x += 4) {
            uint32x4_t dstVector = vld1q_u32(&dst[x]);

            const uint8x16_t dst8 = vreinterpretq_u8_u32(dstVector);

            const uint8x8_t dst8_low = vget_low_u8(dst8);
            const uint8x8_t dst8_high = vget_high_u8(dst8);

            const uint16x8_t dst16_low = vmovl_u8(dst8_low);
            const uint16x8_t dst16_high = vmovl_u8(dst8_high);

            const uint16x8_t result16_low = qvbyte_mul_u16(dst16_low, minusAlphaOfColorVector,
                                                           half);
            const uint16x8_t result16_high = qvbyte_mul_u16(dst16_high, minusAlphaOfColorVector,
                                                            half);

            const uint32x2_t result32_low = vreinterpret_u32_u8(vmovn_u16(result16_low));
            const uint32x2_t result32_high = vreinterpret_u32_u8(vmovn_u16(result16_high));

            uint32x4_t blendedPixels = vcombine_u32(result32_low, result32_high);
            uint32x4_t colorPlusBlendedPixels = vaddq_u32(colorVector, blendedPixels);
            vst1q_u32(&dst[x], colorPlusBlendedPixels);
        }

        SIMD_EPILOGUE(x, length, 3)destPixels[x] =
                                           color + BYTE_MUL(destPixels[x], minusAlphaOfColor);
    }
}

static void
comp_func_solid_SourceOver_neon(uint32_t *destPixels, int length, uint32_t color,
                                uint32_t const_alpha) {
    if ((const_alpha & vAlpha(color)) == 255) {
        memfill32(destPixels, color, length);
    } else {
        if (const_alpha != 255)
            color = BYTE_MUL(color, const_alpha);

        const uint32_t minusAlphaOfColor = vAlpha(~color);
        int x = 0;

        auto *dst = (uint32_t *) destPixels;
        const uint32x4_t colorVector = vdupq_n_u32(color);
        uint16x8_t half = vdupq_n_u16(0x80);
        const uint16x8_t minusAlphaOfColorVector = vdupq_n_u16(minusAlphaOfColor);

        for (; x < length - 3; x += 4) {
            uint32x4_t dstVector = vld1q_u32(&dst[x]);

            const uint8x16_t dst8 = vreinterpretq_u8_u32(dstVector);

            const uint8x8_t dst8_low = vget_low_u8(dst8);
            const uint8x8_t dst8_high = vget_high_u8(dst8);

            const uint16x8_t dst16_low = vmovl_u8(dst8_low);
            const uint16x8_t dst16_high = vmovl_u8(dst8_high);

            const uint16x8_t result16_low = qvbyte_mul_u16(dst16_low, minusAlphaOfColorVector,
                                                           half);
            const uint16x8_t result16_high = qvbyte_mul_u16(dst16_high, minusAlphaOfColorVector,
                                                            half);

            const uint32x2_t result32_low = vreinterpret_u32_u8(vmovn_u16(result16_low));
            const uint32x2_t result32_high = vreinterpret_u32_u8(vmovn_u16(result16_high));

            uint32x4_t blendedPixels = vcombine_u32(result32_low, result32_high);
            uint32x4_t colorPlusBlendedPixels = vaddq_u32(colorVector, blendedPixels);
            vst1q_u32(&dst[x], colorPlusBlendedPixels);
        }

        SIMD_EPILOGUE(x, length, 3)destPixels[x] =
                                           color + BYTE_MUL(destPixels[x], minusAlphaOfColor);
    }
}

static inline uint INTERPOLATE_PIXEL_255(uint x, uint a, uint y, uint b) {
    uint t = (x & 0xff00ff) * a + (y & 0xff00ff) * b;
    t = (t + ((t >> 8) & 0xff00ff) + 0x800080) >> 8;
    t &= 0xff00ff;

    x = ((x >> 8) & 0xff00ff) * a + ((y >> 8) & 0xff00ff) * b;
    x = (x + ((x >> 8) & 0xff00ff) + 0x800080);
    x &= 0xff00ff00;
    x |= t;
    return x;
}

static const uint AMASK = 0xff000000;
static const uint RMASK = 0x00ff0000;
static const uint GMASK = 0x0000ff00;
static const uint BMASK = 0x000000ff;

#if __aarch64__ // 64-bit versions
#define AMIX(mask) (vMin(((uint64_t(s)&(mask)) + (uint64_t(d)&(mask))), uint64_t(mask)))
#define MIX(mask) (vMin(((uint64_t(s)&(mask)) + (uint64_t(d)&(mask))), uint64_t(mask)))
#else // 32 bits
// The mask for alpha can overflow over 32 bits
#define AMIX(mask) uint32_t(vMin(((uint64_t(s)&(mask)) + (uint64_t(d)&(mask))), uint64_t(mask)))
#define MIX(mask) (vMin(((uint32_t(s)&(mask)) + (uint32_t(d)&(mask))), uint32_t(mask)))
#endif

inline uint32_t
comp_func_Plus_one_pixel_const_alpha(uint32_t d, const uint32_t s, const uint32_t const_alpha,
                                     const uint32_t one_minus_const_alpha) {
    const uint32_t result = uint32_t(AMIX(AMASK) | MIX(RMASK) | MIX(GMASK) | MIX(BMASK));
    return INTERPOLATE_PIXEL_255(result, const_alpha, d, one_minus_const_alpha);
}

inline uint32_t comp_func_Plus_one_pixel(uint32_t d, const uint32_t s) {
    const uint32_t result = uint32_t(AMIX(AMASK) | MIX(RMASK) | MIX(GMASK) | MIX(BMASK));
    return result;
}

static inline uint16x8_t
qvinterpolate_pixel_255(uint16x8_t x, uint16x8_t a, uint16x8_t y, uint16x8_t b, uint16x8_t half) {
    // t = x * a + y * b

    const uint16x8_t ta = vmulq_u16(x, a);
    const uint16x8_t tb = vmulq_u16(y, b);

    return qvdiv_255_u16(vaddq_u16(ta, tb), half);
}

void comp_func_Plus_neon(uint32_t *dst, int length, const uint32_t *src, uint32_t const_alpha) {
    if (const_alpha == 255) {
        uint *const end = dst + length;
        uint *const neonEnd = end - 3;

        while (dst < neonEnd) {
            uint8x16_t vs = vld1q_u8((const uint8_t *) src);
            const uint8x16_t vd = vld1q_u8((uint8_t *) dst);
            vs = vqaddq_u8(vs, vd);
            vst1q_u8((uint8_t *) dst, vs);
            src += 4;
            dst += 4;
        }

        while (dst != end) {
            *dst = comp_func_Plus_one_pixel(*dst, *src);
            ++dst;
            ++src;
        }
    } else {
        int x = 0;
        const int one_minus_const_alpha = 255 - (int) const_alpha;
        const uint16x8_t constAlphaVector = vdupq_n_u16(const_alpha);
        const uint16x8_t oneMinusconstAlphaVector = vdupq_n_u16(one_minus_const_alpha);

        const uint16x8_t half = vdupq_n_u16(0x80);
        for (; x < length - 3; x += 4) {
            const uint32x4_t src32 = vld1q_u32((uint32_t *) &src[x]);
            const uint8x16_t src8 = vreinterpretq_u8_u32(src32);
            uint8x16_t dst8 = vld1q_u8((uint8_t *) &dst[x]);
            uint8x16_t result = vqaddq_u8(dst8, src8);

            uint16x8_t result_low = vmovl_u8(vget_low_u8(result));
            uint16x8_t result_high = vmovl_u8(vget_high_u8(result));

            uint16x8_t dst_low = vmovl_u8(vget_low_u8(dst8));
            uint16x8_t dst_high = vmovl_u8(vget_high_u8(dst8));

            result_low = qvinterpolate_pixel_255(result_low, constAlphaVector, dst_low,
                                                 oneMinusconstAlphaVector, half);
            result_high = qvinterpolate_pixel_255(result_high, constAlphaVector, dst_high,
                                                  oneMinusconstAlphaVector, half);

            const uint32x2_t result32_low = vreinterpret_u32_u8(vmovn_u16(result_low));
            const uint32x2_t result32_high = vreinterpret_u32_u8(vmovn_u16(result_high));
            vst1q_u32((uint32_t *) &dst[x], vcombine_u32(result32_low, result32_high));
        }

        SIMD_EPILOGUE(x, length, 3)dst[x] = comp_func_Plus_one_pixel_const_alpha(dst[x], src[x],
                                                                                 const_alpha,
                                                                                 one_minus_const_alpha);
    }
}

void RenderFuncTable::neon() {
    updateColor(BlendMode::Src, comp_func_solid_Source_neon);
    updateColor(BlendMode::SrcOver, comp_func_solid_SourceOver_neon);

    updateSrc(BlendMode::Src, comp_func_Plus_neon);
}

#endif
