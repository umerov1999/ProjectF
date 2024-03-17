#if defined(__ARM_NEON__) || defined(__aarch64__)

#include <arm_neon.h>

#include "vdrawhelper.h"

#define _neon_define0(type, s, body) \
    __extension__({                      \
        type _a = (s);                   \
        body                             \
    })

#define _neon_unlikely(x) __builtin_expect(!!(x), 0)

#define _neon_mm_srli_epi32(a, imm)                                                \
    _neon_define0(                                                        \
        int64x2_t, a, int64x2_t ret; if (_neon_unlikely((imm) & ~31)) {   \
            ret = vreinterpretq_s64_s32(vdupq_n_s32(0));                      \
        } else {                                                              \
            ret = vreinterpretq_s64_u32(                                      \
                vshlq_u32(vreinterpretq_u32_s64(_a), vdupq_n_s32(-(imm))));   \
        } ret;)

#define _neon_mm_srli_epi64(a, imm)                                                \
    _neon_define0(                                                        \
        int64x2_t, a, int64x2_t ret; if (_neon_unlikely((imm) & ~63)) {       \
            ret = vreinterpretq_s64_s32(vdupq_n_s32(0));                                        \
        } else {                                                              \
            ret = vreinterpretq_s64_u64(                                    \
                vshlq_u64(vreinterpretq_u64_s64(_a), vdupq_n_s64(-(imm)))); \
        } ret;)

inline static int64x2_t _neon_mm_unpacklo_epi16(int64x2_t a, int64x2_t b) {
#if defined(__aarch64__) || defined(_M_ARM64)
    return vreinterpretq_s64_s16(vzip1q_s16(vreinterpretq_s16_s64(a), vreinterpretq_s16_s64(b)));
#else
    int16x4_t a1 = vget_low_s16(vreinterpretq_s16_s64(a));
    int16x4_t b1 = vget_low_s16(vreinterpretq_s16_s64(b));
    int16x4x2_t result = vzip_s16(a1, b1);
    return vreinterpretq_s64_s16(vcombine_s16(result.val[0], result.val[1]));
#endif
}

inline static int64x2_t _neon_mm_unpackhi_epi16(int64x2_t a, int64x2_t b) {
#if defined(__aarch64__) || defined(_M_ARM64)
    return vreinterpretq_s64_s16(vzip2q_s16(vreinterpretq_s16_s64(a), vreinterpretq_s16_s64(b)));
#else
    int16x4_t a1 = vget_high_s16(vreinterpretq_s16_s64(a));
    int16x4_t b1 = vget_high_s16(vreinterpretq_s16_s64(b));
    int16x4x2_t result = vzip_s16(a1, b1);
    return vreinterpretq_s64_s16(vcombine_s16(result.val[0], result.val[1]));
#endif
}

inline static int64x2_t _neon_mm_packus_epi16(const int64x2_t a, const int64x2_t b) {
    return vreinterpretq_s64_u8(
            vcombine_u8(vqmovun_s16(vreinterpretq_s16_s64(a)),
                        vqmovun_s16(vreinterpretq_s16_s64(b))));
}

inline static int64x2_t _neon_mm_slli_epi64(int64x2_t a, int imm) {
    if (_neon_unlikely(imm & ~63))
        return vreinterpretq_s64_s32(vdupq_n_s32(0));
    return vshlq_s64(a, vdupq_n_s64(imm));
}

inline static int64x2_t _neon_mm_unpacklo_epi8(int64x2_t a, int64x2_t b) {
#if defined(__aarch64__) || defined(_M_ARM64)
    return vreinterpretq_s64_s8(
            vzip1q_s8(vreinterpretq_s8_s64(a), vreinterpretq_s8_s64(b)));
#else
    int8x8_t a1 = vreinterpret_s8_s16(vget_low_s16(vreinterpretq_s16_s64(a)));
    int8x8_t b1 = vreinterpret_s8_s16(vget_low_s16(vreinterpretq_s16_s64(b)));
    int8x8x2_t result = vzip_s8(a1, b1);
    return vreinterpretq_s64_s8(vcombine_s8(result.val[0], result.val[1]));
#endif
}

inline static int64x2_t _neon_mm_slli_epi16(int64x2_t a, int imm) {
    if (_neon_unlikely(imm & ~15))
        return vreinterpretq_s64_s32(vdupq_n_s32(0));
    return vreinterpretq_s64_s16(
            vshlq_s16(vreinterpretq_s16_s64(a), vdupq_n_s16(imm)));
}

inline static int64x2_t _neon_mm_unpackhi_epi8(int64x2_t a, int64x2_t b) {
#if defined(__aarch64__) || defined(_M_ARM64)
    return vreinterpretq_s64_s8(vzip2q_s8(vreinterpretq_s8_s64(a), vreinterpretq_s8_s64(b)));
#else
    int8x8_t a1 =
        vreinterpret_s8_s16(vget_high_s16(vreinterpretq_s16_s64(a)));
    int8x8_t b1 =
        vreinterpret_s8_s16(vget_high_s16(vreinterpretq_s16_s64(b)));
    int8x8x2_t result = vzip_s8(a1, b1);
    return vreinterpretq_s64_s8(vcombine_s8(result.val[0], result.val[1]));
#endif
}

inline static int64x2_t _neon_mm_set_epi32(int i3, int i2, int i1, int i0) {
    int32_t __attribute__((aligned(16))) data[4] = {i0, i1, i2, i3};
    return vreinterpretq_s64_s32(vld1q_s32(data));
}

// Each 32bits components of alphaChannel must be in the form 0x00AA00AA
inline static int64x2_t v4_byte_mul_neon(int64x2_t c, int64x2_t a) {
    const int64x2_t ag_mask = vreinterpretq_s64_s32(vdupq_n_s32(0xFF00FF00));
    const int64x2_t rb_mask = vreinterpretq_s64_s32(vdupq_n_s32(0x00FF00FF));

    /* for AG */
    int64x2_t v_ag = vreinterpretq_s64_s32(
            vandq_s32(vreinterpretq_s32_s64(ag_mask), vreinterpretq_s32_s64(c)));
    v_ag = _neon_mm_srli_epi32(v_ag, 8);
    v_ag = vreinterpretq_s64_s16(vmulq_s16(vreinterpretq_s16_s64(a), vreinterpretq_s16_s64(v_ag)));
    v_ag = vreinterpretq_s64_s32(
            vandq_s32(vreinterpretq_s32_s64(ag_mask), vreinterpretq_s32_s64(v_ag)));

    /* for RB */
    int64x2_t v_rb = vreinterpretq_s64_s32(
            vandq_s32(vreinterpretq_s32_s64(rb_mask), vreinterpretq_s32_s64(c)));
    v_rb = vreinterpretq_s64_s16(vmulq_s16(vreinterpretq_s16_s64(a), vreinterpretq_s16_s64(v_rb)));
    v_rb = _neon_mm_srli_epi32(v_rb, 8);
    v_rb = vreinterpretq_s64_s32(
            vandq_s32(vreinterpretq_s32_s64(rb_mask), vreinterpretq_s32_s64(v_rb)));

    /* combine */
    return vreinterpretq_s64_s32(
            vaddq_s32(vreinterpretq_s32_s64(v_ag), vreinterpretq_s32_s64(v_rb)));
}

#define _neon_shuffle(type, a, b, ...) \
    __builtin_shufflevector(a, b, __VA_ARGS__)

#define _neon_mm_shuffle_ps(a, b, imm)                                              \
    __extension__({                                                            \
        float32x4_t _input1 = a;                       \
        float32x4_t _input2 = b;                       \
        float32x4_t _shuf =                                                    \
            _neon_shuffle(int32x4_t, _input1, _input2, (imm) & (0x3), ((imm) >> 2) & 0x3, \
                          (((imm) >> 4) & 0x3) + 4, (((imm) >> 6) & 0x3) + 4); \
        _shuf;                                         \
    })

static inline int64x2_t v4_interpolate_color_neon(int64x2_t a, int64x2_t c0,
                                                  int64x2_t c1) {
    const int64x2_t rb_mask = vreinterpretq_s64_s32(vdupq_n_s32(0xFF00FF00));
    const int64x2_t zero = vreinterpretq_s64_s32(vdupq_n_s32(0));

    int64x2_t a_l = a;
    int64x2_t a_h = a;
    a_l = _neon_mm_unpacklo_epi16(a_l, a_l);
    a_h = _neon_mm_unpackhi_epi16(a_h, a_h);

    int64x2_t a_t = _neon_mm_slli_epi64(a_l, 32);
    int64x2_t a_t0 = _neon_mm_slli_epi64(a_h, 32);

    a_l = vreinterpretq_s64_s32(vaddq_s32(vreinterpretq_s32_s64(a_l), vreinterpretq_s32_s64(a_t)));
    a_h = vreinterpretq_s64_s32(vaddq_s32(vreinterpretq_s32_s64(a_h), vreinterpretq_s32_s64(a_t0)));

    int64x2_t c0_l = c0;
    int64x2_t c0_h = c0;

    c0_l = _neon_mm_unpacklo_epi8(c0_l, zero);
    c0_h = _neon_mm_unpackhi_epi8(c0_h, zero);

    int64x2_t c1_l = c1;
    int64x2_t c1_h = c1;

    c1_l = _neon_mm_unpacklo_epi8(c1_l, zero);
    c1_h = _neon_mm_unpackhi_epi8(c1_h, zero);

    int64x2_t cl_sub = vreinterpretq_s64_s16(
            vsubq_s16(vreinterpretq_s16_s64(c0_l), vreinterpretq_s16_s64(c1_l)));
    int64x2_t ch_sub = vreinterpretq_s64_s16(
            vsubq_s16(vreinterpretq_s16_s64(c0_h), vreinterpretq_s16_s64(c1_h)));

    cl_sub = vreinterpretq_s64_s16(
            vmulq_s16(vreinterpretq_s16_s64(cl_sub), vreinterpretq_s16_s64(a_l)));
    ch_sub = vreinterpretq_s64_s16(
            vmulq_s16(vreinterpretq_s16_s64(ch_sub), vreinterpretq_s16_s64(a_h)));

    int64x2_t c1ls = _neon_mm_slli_epi16(c1_l, 8);
    int64x2_t c1hs = _neon_mm_slli_epi16(c1_h, 8);

    cl_sub = vreinterpretq_s64_s16(
            vaddq_s16(vreinterpretq_s16_s64(cl_sub), vreinterpretq_s16_s64(c1ls)));
    ch_sub = vreinterpretq_s64_s16(
            vaddq_s16(vreinterpretq_s16_s64(ch_sub), vreinterpretq_s16_s64(c1hs)));

    cl_sub = vreinterpretq_s64_s32(
            vandq_s32(vreinterpretq_s32_s64(cl_sub), vreinterpretq_s32_s64(rb_mask)));
    ch_sub = vreinterpretq_s64_s32(
            vandq_s32(vreinterpretq_s32_s64(ch_sub), vreinterpretq_s32_s64(rb_mask)));

    cl_sub = _neon_mm_srli_epi64(cl_sub, 8);
    ch_sub = _neon_mm_srli_epi64(ch_sub, 8);

    cl_sub = _neon_mm_packus_epi16(cl_sub, cl_sub);
    ch_sub = _neon_mm_packus_epi16(ch_sub, ch_sub);

    return (int64x2_t) _neon_mm_shuffle_ps((float32x4_t) cl_sub, (float32x4_t) ch_sub, 0x44);
}

// Load src and dest vector
#define V4_FETCH_SRC_DEST                           \
    int64x2_t v_src = vreinterpretq_s64_s32(vld1q_s32((const int32_t *) src)); \
    int64x2_t v_dest = vreinterpretq_s64_s32(vld1q_s32((const int32_t *) dest));

#define V4_FETCH_SRC int64x2_t v_src = vreinterpretq_s64_s32(vld1q_s32((const int32_t *) src));;

#define V4_STORE_DEST vst1q_s32((int32_t *) dest, vreinterpretq_s32_s64(v_src));

#define V4_SRC_DEST_LEN_INC \
    dest += 4;              \
    src += 4;               \
    length -= 4;

// Multiply src color with const_alpha
#define V4_ALPHA_MULTIPLY v_src = v4_byte_mul_neon(v_src, v_alpha);


// dest = src + dest * sia
#define V4_COMP_OP_SRC \
    v_src = v4_interpolate_color_neon(v_alpha, v_src, v_dest);

#define LOOP_ALIGNED_U1_A4(DEST, LENGTH, UOP, A4OP) \
    {                                               \
        while ((uintptr_t)DEST & 0xF && LENGTH)     \
            UOP                                     \
                                                    \
                while (LENGTH)                      \
            {                                       \
                switch (LENGTH) {                   \
                case 3:                             \
                case 2:                             \
                case 1:                             \
                    UOP break;                      \
                default:                            \
                    A4OP break;                     \
                }                                   \
            }                                       \
    }

void memfill32(uint32_t *dest, uint32_t value, int length) {
    int64x2_t vector_data = _neon_mm_set_epi32(value, value, value, value);

    // run till memory alligned to 16byte memory
    while (length && ((uintptr_t) dest & 0xf)) {
        *dest++ = value;
        length--;
    }

    while (length >= 32) {
        vst1q_s32((int32_t *) (dest), vreinterpretq_s32_s64(vector_data));
        vst1q_s32((int32_t *) (dest + 4), vreinterpretq_s32_s64(vector_data));
        vst1q_s32((int32_t *) (dest + 8), vreinterpretq_s32_s64(vector_data));
        vst1q_s32((int32_t *) (dest + 12), vreinterpretq_s32_s64(vector_data));
        vst1q_s32((int32_t *) (dest + 16), vreinterpretq_s32_s64(vector_data));
        vst1q_s32((int32_t *) (dest + 20), vreinterpretq_s32_s64(vector_data));
        vst1q_s32((int32_t *) (dest + 24), vreinterpretq_s32_s64(vector_data));
        vst1q_s32((int32_t *) (dest + 28), vreinterpretq_s32_s64(vector_data));

        dest += 32;
        length -= 32;
    }

    if (length >= 16) {
        vst1q_s32((int32_t *) (dest), vreinterpretq_s32_s64(vector_data));
        vst1q_s32((int32_t *) (dest + 4), vreinterpretq_s32_s64(vector_data));
        vst1q_s32((int32_t *) (dest + 8), vreinterpretq_s32_s64(vector_data));
        vst1q_s32((int32_t *) (dest + 12), vreinterpretq_s32_s64(vector_data));

        dest += 16;
        length -= 16;
    }

    if (length >= 8) {
        vst1q_s32((int32_t *) (dest), vreinterpretq_s32_s64(vector_data));
        vst1q_s32((int32_t *) (dest + 4), vreinterpretq_s32_s64(vector_data));

        dest += 8;
        length -= 8;
    }

    if (length >= 4) {
        vst1q_s32((int32_t *) (dest), vreinterpretq_s32_s64(vector_data));

        dest += 4;
        length -= 4;
    }

    while (length) {
        *dest++ = value;
        length--;
    }
}

// dest = color + (dest * alpha)
inline static void copy_helper_neon(uint32_t *dest, int length,
                                    uint32_t color, uint32_t alpha) {
    const int64x2_t v_color = vreinterpretq_s64_s32(vdupq_n_s32(color));
    const int64x2_t v_a = vreinterpretq_s64_s16(vdupq_n_s16(alpha));

    LOOP_ALIGNED_U1_A4(dest, length,
                       { /* UOP */
                           *dest = color + BYTE_MUL(*dest, alpha);
                           dest++;
                           length--;
                       },
                       { /* A4OP */
                           int64x2_t v_dest = vreinterpretq_s64_s32(
                                   vld1q_s32((const int32_t *) dest));

                           v_dest = v4_byte_mul_neon(v_dest, v_a);
                           v_dest = vreinterpretq_s64_s32(vaddq_s32(vreinterpretq_s32_s64(v_dest),
                                                                    vreinterpretq_s32_s64(
                                                                            v_color)));

                           vst1q_s32((int32_t *) dest, vreinterpretq_s32_s64(v_dest));

                           dest += 4;
                           length -= 4;
                       })
}

static void color_Source(uint32_t *dest, int length, uint32_t color,
                         uint32_t const_alpha) {
    if (const_alpha == 255) {
        memfill32(dest, color, length);
    } else {
        int ialpha;

        ialpha = 255 - const_alpha;
        color = BYTE_MUL(color, const_alpha);
        copy_helper_neon(dest, length, color, ialpha);
    }
}

static void color_SourceOver(uint32_t *dest, int length,
                             uint32_t color,
                             uint32_t const_alpha) {
    int ialpha;

    if (const_alpha != 255) color = BYTE_MUL(color, const_alpha);
    ialpha = 255 - vAlpha(color);
    copy_helper_neon(dest, length, color, ialpha);
}

static void src_Source(uint32_t *dest, int length, const uint32_t *src,
                       uint32_t const_alpha) {
    int ialpha;
    if (const_alpha == 255) {
        memcpy(dest, src, length * sizeof(uint32_t));
    } else {
        ialpha = 255 - const_alpha;
        int64x2_t v_alpha = vreinterpretq_s64_s32(vdupq_n_s32(const_alpha));

        LOOP_ALIGNED_U1_A4(dest, length,
                           { /* UOP */
                               *dest = interpolate_pixel(*src, const_alpha,
                                                         *dest, ialpha);
                               dest++;
                               src++;
                               length--;
                           },
                           {/* A4OP */
                               V4_FETCH_SRC_DEST
                               V4_COMP_OP_SRC V4_STORE_DEST
                               V4_SRC_DEST_LEN_INC
                           })
    }
}

void RenderFuncTable::neon() {
    updateColor(BlendMode::Src, color_Source);
    updateColor(BlendMode::SrcOver, color_SourceOver);

    updateSrc(BlendMode::Src, src_Source);
}

#endif