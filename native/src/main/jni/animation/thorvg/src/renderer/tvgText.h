/*
 * Copyright (c) 2023 - 2025 the ThorVG project. All rights reserved.

 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:

 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.

 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

#ifndef _TVG_TEXT_H
#define _TVG_TEXT_H

#include "tvgCommon.h"
#include "tvgShape.h"
#include "tvgFill.h"
#include "tvgLoader.h"

#define TEXT(A) PIMPL(A, Text)

struct Text::Impl : Paint::Impl
{
    Shape* shape;   //text shape
    FontLoader* loader = nullptr;
    char* utf8 = nullptr;
    float fontSize;
    bool italic = false;
    bool changed = false;

    Impl(Text* p) : Paint::Impl(p), shape(Shape::gen())
    {
    }

    ~Impl()
    {
        tvg::free(utf8);
        LoaderMgr::retrieve(loader);
        delete(shape);
    }

    Result text(const char* utf8)
    {
        tvg::free(this->utf8);
        if (utf8) this->utf8 = strdup(utf8);
        else this->utf8 = nullptr;
        changed = true;

        return Result::Success;
    }

    Result font(const char* name, float size, const char* style)
    {
        auto loader = name ? LoaderMgr::loader(name, nullptr) : LoaderMgr::anyfont();
        if (!loader) return Result::InsufficientCondition;

        if (style && strstr(style, "italic")) italic = true;
        else italic = false;

        fontSize = size;

        //Same resource has been loaded.
        if (this->loader == loader) {
            this->loader->sharing--;  //make it sure the reference counting.
            return Result::Success;
        } else if (this->loader) {
            LoaderMgr::retrieve(this->loader);
        }
        this->loader = static_cast<FontLoader*>(loader);

        changed = true;
        return Result::Success;
    }

    RenderRegion bounds(RenderMethod* renderer)
    {
        return SHAPE(shape)->bounds(renderer);
    }

    bool render(RenderMethod* renderer)
    {
        if (!loader) return true;
        renderer->blend(blendMethod);
        return PAINT(shape)->render(renderer);
    }

    bool load()
    {
        if (!loader) return false;

        loader->request(shape, utf8);
        //reload
        if (changed) {
            loader->read();
            changed = false;
        }
        return loader->transform(shape, fontSize, italic);
    }

    RenderData update(RenderMethod* renderer, const Matrix& transform, Array<RenderData>& clips, uint8_t opacity, RenderUpdateFlag pFlag, TVG_UNUSED bool clipper)
    {
        if (!load()) return nullptr;

        //transform the gradient coordinates based on the final scaled font.
        auto fill = SHAPE(shape)->rs.fill;
        if (fill && SHAPE(shape)->renderFlag & RenderUpdateFlag::Gradient) {
            auto scale = 1.0f / loader->scale;
            if (fill->type() == Type::LinearGradient) {
                LINEAR(fill)->x1 *= scale;
                LINEAR(fill)->y1 *= scale;
                LINEAR(fill)->x2 *= scale;
                LINEAR(fill)->y2 *= scale;
            } else {
                RADIAL(fill)->cx *= scale;
                RADIAL(fill)->cy *= scale;
                RADIAL(fill)->r *= scale;
                RADIAL(fill)->fx *= scale;
                RADIAL(fill)->fy *= scale;
                RADIAL(fill)->fr *= scale;
            }
        }

        return PAINT(shape)->update(renderer, transform, clips, opacity, pFlag, false);
    }

    bool bounds(float* x, float* y, float* w, float* h, TVG_UNUSED bool stroking)
    {
        if (!load()) return false;
        PAINT(shape)->bounds(x, y, w, h, true, true, false);
        return true;
    }

    Paint* duplicate(Paint* ret)
    {
        if (ret) TVGERR("RENDERER", "TODO: duplicate()");

        load();

        auto text = Text::gen();
        auto dup = TEXT(text);
        SHAPE(shape)->duplicate(dup->shape);

        if (loader) {
            dup->loader = loader;
            ++dup->loader->sharing;
        }

        dup->utf8 = strdup(utf8);
        dup->italic = italic;
        dup->fontSize = fontSize;

        return text;
    }

    Iterator* iterator()
    {
        return nullptr;
    }
};



#endif //_TVG_TEXT_H
