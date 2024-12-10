#ifndef _ColorReplace_H_
#define _ColorReplace_H_

#include <map>
#include <iostream>

#ifndef TVG_API
#define TVG_API
#endif

namespace tvg {
    class TVG_API hsv_model {
    public:
        hsv_model() {
            h = 0.f;
            s = 0.f;
            v = 0.f;
        }

        float h;
        float s;
        float v;
    };

    class TVG_API rgb_model {
    public:
        rgb_model() {
            r = 0;
            g = 0;
            b = 0;
        }

        explicit rgb_model(int32_t color) {
#ifdef BIG_ENDIAN_COLORS
            r = ((color >> 16) & 0xff);
            g = ((color >> 8) & 0xff);
            b = (color & 0xff);
#else
            r = (color & 0xff);
            g = ((color >> 8) & 0xff);
            b = ((color >> 16) & 0xff);
#endif
        }

        rgb_model(uint8_t iR, uint8_t iG, uint8_t iB) {
            r = iR;
            g = iG;
            b = iB;
        }

        rgb_model(float fR, float fG, float fB) {
            r = (uint8_t) (fR * 255) & 0xff;
            g = (uint8_t) (fG * 255) & 0xff;
            b = (uint8_t) (fB * 255) & 0xff;
        }

        void toFloat(float &fR, float &fG, float &fB) const {
            fR = (float) r / 255.0f;
            fG = (float) g / 255.0f;
            fB = (float) b / 255.0f;
        }

        void toColors(uint8_t &iR, uint8_t &iG, uint8_t &iB) const {
            iR = r;
            iG = g;
            iB = b;
        }

        [[nodiscard]] int32_t toInt32() const {
#ifdef BIG_ENDIAN_COLORS
            return (int32_t)((r << 16) | (g << 8) | b);
#else
            return (int32_t) (r | (g << 8) | (b << 16));
#endif
        }

        uint8_t r;
        uint8_t g;
        uint8_t b;
    };

    static inline uint8_t clamp(float v) {
        if (v < 0.f)
            return 0;
        if (v > 255.f)
            return 255;
        return (uint8_t) v;
    }

    static inline float clampf(float v) {
        if (v < 0.f)
            return 0.f;
        if (v > 1.f)
            return 1.f;
        return v;
    }

    static hsv_model rgb2hsv(const rgb_model &color) {
        hsv_model out;
        float min, max, delta;

        min = color.r < color.g ? color.r : color.g;
        min = min < (float) color.b ? min : (float) color.b;

        max = color.r > color.g ? color.r : color.g;
        max = max > (float) color.b ? max : (float) color.b;

        out.v = max / 255.0f;
        delta = max - min;
        if (delta < 0.00001f) {
            out.s = 0;
            out.h = 0;
            return out;
        }
        if (max > 0.0f) {
            out.s = (delta / max);
        } else {
            out.s = 0.0f;
            out.h = NAN;
            return out;
        }
        if ((float) color.r >= max)
            out.h = ((float) color.g - (float) color.b) / delta;
        else if ((float) color.g >= max)
            out.h = 2.0f + ((float) color.b - (float) color.r) / delta;
        else
            out.h = 4.0f + ((float) color.r - (float) color.g) / delta;

        out.h *= 60.0f;

        if (out.h < 0.0)
            out.h += 360.0f;

        return out;
    }

    static rgb_model
    change_hsv_c(const rgb_model &color, const float fHue, const float fSat, const float fVal) {
        rgb_model out;
        const float cosA = fSat * cos(fHue * 3.14159265f / 180);
        const float sinA = fSat * sin(fHue * 3.14159265f / 180);

        const float aThird = 1.0f / 3.0f;
        const float rootThird = sqrtf(aThird);
        const float oneMinusCosA = (1.0f - cosA);
        const float aThirdOfOneMinusCosA = aThird * oneMinusCosA;
        const float rootThirdTimesSinA = rootThird * sinA;
        const float plus = aThirdOfOneMinusCosA + rootThirdTimesSinA;
        const float minus = aThirdOfOneMinusCosA - rootThirdTimesSinA;

        float matrix[3][3] = {
                {cosA + oneMinusCosA / 3.0f, minus,                       plus},
                {plus,                       cosA + aThirdOfOneMinusCosA, minus},
                {minus,                      plus,                        cosA +
                                                                          aThirdOfOneMinusCosA}
        };
        out.r = clamp(((float) color.r * matrix[0][0] + (float) color.g * matrix[0][1] +
                       (float) color.b * matrix[0][2]) * fVal);
        out.g = clamp(((float) color.r * matrix[1][0] + (float) color.g * matrix[1][1] +
                       (float) color.b * matrix[1][2]) * fVal);
        out.b = clamp(((float) color.r * matrix[2][0] + (float) color.g * matrix[2][1] +
                       (float) color.b * matrix[2][2]) * fVal);
        return out;
    }

    class TVG_API ColorReplace {
    public:
        ColorReplace() {
            useCustomColorsLottieOffset = false;
        }

        ColorReplace &registerCustomColorLottie(int32_t color, int32_t replace) {
            customColorsTableLottie[color] = replace;
            return *this;
        }

        ColorReplace &setUseCustomColorsLottieOffset() {
            useCustomColorsLottieOffset = true;
            return *this;
        }

        void getCustomColorLottie32(int32_t &r, int32_t &g, int32_t &b) {
            auto sr = (uint8_t) r;
            auto sg = (uint8_t) g;
            auto sb = (uint8_t) b;
            getCustomColorLottie(sr, sg, sb);
            r = sr;
            g = sg;
            b = sb;
        }

        void getCustomColorLottie(uint8_t &r, uint8_t &g, uint8_t &b) {
            if (!customColorsTableLottie.empty()) {
                if (useCustomColorsLottieOffset) {
                    switchColorModel(r, g, b);
                    return;
                }
                auto it = customColorsTableLottie.find(rgb_model(r, g, b).toInt32());
                if (it != customColorsTableLottie.end()) {
                    rgb_model(it->second).toColors(r, g, b);
                    return;
                }
            }
        }

        std::map<int32_t, int32_t> customColorsTableLottie;
        bool useCustomColorsLottieOffset;
    private:
        inline void switchColorModel(uint8_t &r, uint8_t &g, uint8_t &b) {
            int32_t from = customColorsTableLottie.begin()->first;
            int32_t to = customColorsTableLottie.begin()->second;

            hsv_model frm = rgb2hsv(rgb_model(from));
            hsv_model tom = rgb2hsv(rgb_model(to));
            hsv_model clrr = rgb2hsv(rgb_model(r, g, b));
            change_hsv_c(rgb_model(r, g, b), tom.h - frm.h, clampf(clrr.s + (tom.s - frm.s)),
                         clampf(clrr.v + (tom.v - frm.v))).toColors(r, g, b);
        }
    };
}
#endif
