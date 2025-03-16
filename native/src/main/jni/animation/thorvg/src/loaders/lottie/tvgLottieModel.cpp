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

#include "tvgMath.h"
#include "tvgTaskScheduler.h"
#include "tvgLottieModel.h"


/************************************************************************/
/* Internal Class Implementation                                        */
/************************************************************************/



/************************************************************************/
/* External Class Implementation                                        */
/************************************************************************/

void LottieSlot::reset()
{
    if (!overridden) return;

    auto shallow = pairs.count == 1 ? true : false;

    ARRAY_FOREACH(pair, pairs) {
        pair->obj->override(pair->prop, shallow, true);
        delete(pair->prop);
        pair->prop = nullptr;
    }
    overridden = false;
}


void LottieSlot::assign(LottieObject* target, bool byDefault, ColorReplace* colorReplacement)
{
    auto copy = !overridden && !byDefault;
    auto shallow = pairs.count == 1 ? true : false;

    //apply slot object to all targets
    ARRAY_FOREACH(pair, pairs) {
        //backup the original properties before overwriting
        switch (type) {
            case LottieProperty::Type::Position: {
                if (copy) pair->prop = new LottieVector(static_cast<LottieTransform*>(pair->obj)->position);
                pair->obj->override(&static_cast<LottieTransform*>(target)->position, shallow, !copy);
                break;
            }
            case LottieProperty::Type::Point: {
                if (copy) pair->prop = new LottieScalar(static_cast<LottieTransform*>(pair->obj)->scale);
                pair->obj->override(&static_cast<LottieTransform*>(target)->scale, shallow, !copy);
                break;
            }
            case LottieProperty::Type::Float: {
                if (copy) pair->prop = new LottieFloat(static_cast<LottieTransform*>(pair->obj)->rotation);
                pair->obj->override(&static_cast<LottieTransform*>(target)->rotation, shallow, !copy);
                break;
            }
            case LottieProperty::Type::Opacity: {
                if (copy) {
                    if (pair->obj->type == LottieObject::Type::Transform) pair->prop = new LottieOpacity(static_cast<LottieTransform*>(pair->obj)->opacity);
                    else pair->prop = new LottieOpacity(static_cast<LottieSolid*>(pair->obj)->opacity);
                }
                pair->obj->override(&static_cast<LottieSolid*>(target)->opacity, shallow, !copy);
                break;
            }
            case LottieProperty::Type::Color: {
                auto color = static_cast<LottieSolid*>(pair->obj)->color;
                colorReplacement->getCustomColorLottie32(color.value.rgb[0], color.value.rgb[1], color.value.rgb[2]);
                if (copy) pair->prop = new LottieColor(color);
                pair->obj->override(&color, shallow, !copy);
                break;
            }
            case LottieProperty::Type::ColorStop: {
                if (copy) pair->prop = new LottieColorStop(static_cast<LottieGradient*>(pair->obj)->colorStops);
                pair->obj->override(&static_cast<LottieGradient*>(target)->colorStops, shallow, !copy);
                break;
            }
            case LottieProperty::Type::TextDoc: {
                if (copy) pair->prop = new LottieTextDoc(static_cast<LottieText*>(pair->obj)->doc);
                pair->obj->override(&static_cast<LottieText*>(target)->doc, shallow, !copy);
                break;
            }
            case LottieProperty::Type::Image: {
                if (copy) pair->prop = new LottieBitmap(static_cast<LottieImage*>(pair->obj)->data);
                pair->obj->override(&static_cast<LottieImage*>(target)->data, shallow, !copy);
                break;
            }
            default: break;
        }
    }
    if (!byDefault) overridden = true;
}


float LottieTextRange::factor(float frameNo, float totalLen, float idx)
{
    auto offset = this->offset(frameNo);
    auto start = this->start(frameNo) + offset;
    auto end = this->end(frameNo) + offset;

    if (random > 0) {
        auto range = end - start;
        auto len = (rangeUnit == Unit::Percent) ? 100.0f : totalLen;
        start = static_cast<float>(random % int(len - range));
        end = start + range;
    }

    auto divisor = (rangeUnit == Unit::Percent) ? (100.0f / totalLen) : 1.0f;
    start /= divisor;
    end /= divisor;

    auto f = 0.0f;

    switch (this->shape) {
        case Square: {
            auto smoothness = this->smoothness(frameNo);
            if (tvg::zero(smoothness)) f = idx >= nearbyintf(start) && idx < nearbyintf(end) ? 1.0f : 0.0f;
            else {
                if (idx >= std::floor(start)) {
                    auto diff = idx - start;
                    f = diff < 0.0f ? std::min(end, 1.0f) + diff : end - idx;
                }
                smoothness *= 0.01f;
                f = (f - (1.0f - smoothness) * 0.5f) / smoothness;
            }
            break;
        }
        case RampUp: {
            f = tvg::equal(start, end) ? (idx >= end ? 1.0f : 0.0f) : (0.5f + idx - start) / (end - start);
            break;
        }
        case RampDown: {
            f = tvg::equal(start, end) ? (idx >= end ? 0.0f : 1.0f) : 1.0f - (0.5f + idx - start) / (end - start);
            break;
        }
        case Triangle: {
            f = tvg::equal(start, end) ? 0.0f : 2.0f * (0.5f + idx - start) / (end - start);
            f = f < 1.0f ? f : 2.0f - f;
            break;
        }
        case Round: {
            idx = tvg::clamp(idx + (0.5f - start), 0.0f, end - start);
            auto range = 0.5f * (end - start);
            auto t = idx - range;
            f = tvg::equal(start, end) ? 0.0f : sqrtf(1.0f - t * t / (range * range));
            break;
        }
        case Smooth: {
            idx = tvg::clamp(idx + (0.5f - start), 0.0f, end - start);
            f = tvg::equal(start, end) ? 0.0f : 0.5f * (1.0f + cosf(MATH_PI * (1.0f + 2.0f * idx / (end - start))));
            break;
        }
    }
    f = tvg::clamp(f, 0.0f, 1.0f);

    //apply easing
    auto minEase = tvg::clamp(this->minEase(frameNo), -100.0f, 100.0f);
    auto maxEase = tvg::clamp(this->maxEase(frameNo), -100.0f, 100.0f);
    if (!tvg::zero(minEase) || !tvg::zero(maxEase)) {
        Point in{1.0f, 1.0f};
        Point out{0.0f, 0.0f};

        if (maxEase > 0.0f) in.x = 1.0f - maxEase * 0.01f;
        else in.y = 1.0f + maxEase * 0.01f;
        if (minEase > 0.0f) out.x = minEase * 0.01f;
        else out.y = -minEase * 0.01f;

        interpolator->set(nullptr, in, out);
        f = interpolator->progress(f);
    }
    f = tvg::clamp(f, 0.0f, 1.0f);

    return f * this->maxAmount(frameNo) * 0.01f;
}


void LottieFont::prepare()
{
    if (!data.b64src || !name) return;

    TaskScheduler::async(false);
    Text::load(name, data.b64src, data.size, "ttf", false);
    TaskScheduler::async(true);
}


void LottieImage::prepare()
{
    LottieObject::type = LottieObject::Image;

    auto picture = Picture::gen();

    //force to load a picture on the same thread
    TaskScheduler::async(false);

    if (data.size > 0) picture->load((const char*)data.b64Data, data.size, data.mimeType);
    else picture->load(data.path);

    TaskScheduler::async(true);

    picture->size(data.width, data.height);
    picture->ref();

    pooler.push(picture);
}


void LottieImage::update()
{
    //Update the picture data
    TaskScheduler::async(false);
    ARRAY_FOREACH(p, pooler) {
        if (data.size > 0) (*p)->load((const char*)data.b64Data, data.size, data.mimeType);
        else (*p)->load(data.path);
        (*p)->size(data.width, data.height);
    }
    TaskScheduler::async(true);
}


void LottieTrimpath::segment(float frameNo, float& start, float& end, Tween& tween, LottieExpressions* exps)
{
    start = tvg::clamp(this->start(frameNo, tween, exps) * 0.01f, 0.0f, 1.0f);
    end = tvg::clamp(this->end(frameNo, tween, exps) * 0.01f, 0.0f, 1.0f);

    auto diff = fabs(start - end);
    if (tvg::zero(diff)) {
        start = 0.0f;
        end = 0.0f;
        return;
    }

    //Even if the start and end values do not cause trimming, an offset > 0 can still affect dashing starting point
    auto o = fmodf(this->offset(frameNo, tween, exps), 360.0f) / 360.0f;  //0 ~ 1
    if (tvg::zero(o) && diff >= 1.0f) {
        start = 0.0f;
        end = 1.0f;
        return;
    }

    if (start > end) std::swap(start, end);
    start += o;
    end += o;
}


uint32_t LottieGradient::populate(ColorStop& color, size_t count)
{
    if (!color.input) return 0;

    uint32_t alphaCnt = (color.input->count - (count * 4)) / 2;
    Array<Fill::ColorStop> output(count + alphaCnt);
    uint32_t cidx = 0;               //color count
    uint32_t clast = count * 4;
    if (clast > color.input->count) clast = color.input->count;
    uint32_t aidx = clast;           //alpha count
    Fill::ColorStop cs;

    //merge color stops.
    for (uint32_t i = 0; i < color.input->count; ++i) {
        if (cidx == clast || aidx == color.input->count) break;
        if ((*color.input)[cidx] == (*color.input)[aidx]) {
            cs.offset = (*color.input)[cidx];
            cs.r = (uint8_t)nearbyint((*color.input)[cidx + 1] * 255.0f);
            cs.g = (uint8_t)nearbyint((*color.input)[cidx + 2] * 255.0f);
            cs.b = (uint8_t)nearbyint((*color.input)[cidx + 3] * 255.0f);
            cs.a = (uint8_t)nearbyint((*color.input)[aidx + 1] * 255.0f);
            cidx += 4;
            aidx += 2;
        } else if ((*color.input)[cidx] < (*color.input)[aidx]) {
            cs.offset = (*color.input)[cidx];
            cs.r = (uint8_t)nearbyint((*color.input)[cidx + 1] * 255.0f);
            cs.g = (uint8_t)nearbyint((*color.input)[cidx + 2] * 255.0f);
            cs.b = (uint8_t)nearbyint((*color.input)[cidx + 3] * 255.0f);
            //generate alpha value
            if (output.count > 0) {
                auto p = ((*color.input)[cidx] - output.last().offset) / ((*color.input)[aidx] - output.last().offset);
                cs.a = lerp<uint8_t>(output.last().a, (uint8_t)nearbyint((*color.input)[aidx + 1] * 255.0f), p);
            } else cs.a = (uint8_t)nearbyint((*color.input)[aidx + 1] * 255.0f);
            cidx += 4;
        } else {
            cs.offset = (*color.input)[aidx];
            cs.a = (uint8_t)nearbyint((*color.input)[aidx + 1] * 255.0f);
            //generate color value
            if (output.count > 0) {
                auto p = ((*color.input)[aidx] - output.last().offset) / ((*color.input)[cidx] - output.last().offset);
                cs.r = lerp<uint8_t>(output.last().r, (uint8_t)nearbyint((*color.input)[cidx + 1] * 255.0f), p);
                cs.g = lerp<uint8_t>(output.last().g, (uint8_t)nearbyint((*color.input)[cidx + 2] * 255.0f), p);
                cs.b = lerp<uint8_t>(output.last().b, (uint8_t)nearbyint((*color.input)[cidx + 3] * 255.0f), p);
            } else {
                cs.r = (uint8_t)nearbyint((*color.input)[cidx + 1] * 255.0f);
                cs.g = (uint8_t)nearbyint((*color.input)[cidx + 2] * 255.0f);
                cs.b = (uint8_t)nearbyint((*color.input)[cidx + 3] * 255.0f);
            }
            aidx += 2;
        }
        if (cs.a < 255) opaque = false;
        colorReplacement->getCustomColorLottie(cs.r, cs.g, cs.b);
        output.push(cs);
    }

    //color remains
    while (cidx + 3 < clast) {
        cs.offset = (*color.input)[cidx];
        cs.r = (uint8_t)nearbyint((*color.input)[cidx + 1] * 255.0f);
        cs.g = (uint8_t)nearbyint((*color.input)[cidx + 2] * 255.0f);
        cs.b = (uint8_t)nearbyint((*color.input)[cidx + 3] * 255.0f);
        cs.a = (output.count > 0) ? output.last().a : 255;
        if (cs.a < 255) opaque = false;
        colorReplacement->getCustomColorLottie(cs.r, cs.g, cs.b);
        output.push(cs);
        cidx += 4;
    }

    //alpha remains
    while (aidx < color.input->count) {
        cs.offset = (*color.input)[aidx];
        cs.a = (uint8_t)nearbyint((*color.input)[aidx + 1] * 255.0f);
        if (cs.a < 255) opaque = false;
        if (output.count > 0) {
            cs.r = output.last().r;
            cs.g = output.last().g;
            cs.b = output.last().b;
        } else cs.r = cs.g = cs.b = 255;
        colorReplacement->getCustomColorLottie(cs.r, cs.g, cs.b);
        output.push(cs);
        aidx += 2;
    }

    color.data = output.data;
    output.data = nullptr;

    color.input->reset();
    delete(color.input);

    return output.count;
}


Fill* LottieGradient::fill(float frameNo, uint8_t opacity, Tween& tween, LottieExpressions* exps)
{
    if (opacity == 0) return nullptr;

    Fill* fill;
    auto s = start(frameNo, tween, exps);
    auto e = end(frameNo, tween, exps);

    //Linear Graident
    if (id == 1) {
        fill = LinearGradient::gen();
        static_cast<LinearGradient*>(fill)->linear(s.x, s.y, e.x, e.y);
    //Radial Gradient
    } else {
        fill = RadialGradient::gen();
        auto w = fabsf(e.x - s.x);
        auto h = fabsf(e.y - s.y);
        auto r = (w > h) ? (w + 0.375f * h) : (h + 0.375f * w);
        auto progress = this->height(frameNo, tween, exps) * 0.01f;

        if (tvg::zero(progress)) {
            static_cast<RadialGradient*>(fill)->radial(s.x, s.y, r, s.x, s.y, 0.0f);
        } else {
            if (tvg::equal(progress, 1.0f)) progress = 0.99f;
            auto startAngle = rad2deg(tvg::atan2(e.y - s.y, e.x - s.x));
            auto angle = deg2rad((startAngle + this->angle(frameNo, tween, exps)));
            auto fx = s.x + cos(angle) * progress * r;
            auto fy = s.y + sin(angle) * progress * r;
            // Lottie doesn't have any focal radius concept
            static_cast<RadialGradient*>(fill)->radial(s.x, s.y, r, fx, fy, 0.0f);
        }
    }

    colorStops(frameNo, fill, tween, exps);

    //multiply the current opacity with the fill
    if (opacity < 255) {
        const Fill::ColorStop* colorStops;
        auto cnt = fill->colorStops(&colorStops);
        for (uint32_t i = 0; i < cnt; ++i) {
            const_cast<Fill::ColorStop*>(&colorStops[i])->a = MULTIPLY(colorStops[i].a, opacity);
        }
    }

    return fill;
}


LottieGroup::LottieGroup()
{
    reqFragment = false;
    buildDone = false;
    trimpath = false;
    visible = false;
    allowMerge = true;
}


void LottieGroup::prepare(LottieObject::Type type)
{
    LottieObject::type = type;

    if (children.count == 0) return;

    size_t strokeCnt = 0;
    size_t fillCnt = 0;

    ARRAY_REVERSE_FOREACH(c, children) {
        auto child = static_cast<LottieObject*>(*c);

        if (child->type == LottieObject::Type::Trimpath) trimpath = true;

        /* Figure out if this group is a simple path drawing.
           In that case, the rendering context can be sharable with the parent's. */
        if (allowMerge && (child->type == LottieObject::Group || !child->mergeable())) allowMerge = false;

        //Figure out this group has visible contents
        switch (child->type) {
            case LottieObject::Group: {
                visible |= static_cast<LottieGroup*>(child)->visible;
                break;
            }
            case LottieObject::Rect:
            case LottieObject::Ellipse:
            case LottieObject::Path:
            case LottieObject::Polystar:
            case LottieObject::Image:
            case LottieObject::Text: {
                visible = true;
                break;
            }
            default: break;
        }

        if (reqFragment) continue;

        /* Figure out if the rendering context should be fragmented.
           Multiple stroking or grouping with a stroking would occur this.
           This fragment resolves the overlapped stroke outlines. */
        if (child->type == LottieObject::Group && !child->mergeable()) {
            if (strokeCnt > 0 || fillCnt > 0) reqFragment = true;
        } else if (child->type == LottieObject::SolidStroke || child->type == LottieObject::GradientStroke) {
            if (strokeCnt > 0) reqFragment = true;
            else ++strokeCnt;
        } else if (child->type == LottieObject::SolidFill || child->type == LottieObject::GradientFill) {
            if (fillCnt > 0) reqFragment = true;
            else ++fillCnt;
        }
    }

    //Reverse the drawing order if this group has a trimpath.
    if (!trimpath) return;

    for (uint32_t i = 0; i < children.count - 1; ) {
        auto child2 = children[i + 1];
        if (!child2->mergeable() || child2->type == LottieObject::Transform) {
            i += 2;
            continue;
        }
        auto child = children[i];
        if (!child->mergeable() || child->type == LottieObject::Transform) {
            i++;
            continue;
        }
        children[i] = child2;
        children[i + 1] = child;
        i++;
    }
}


LottieLayer::~LottieLayer()
{
    //No need to free assets children because the Composition owns them.
    if (rid) children.clear();

    ARRAY_FOREACH(p, masks) delete(*p);
    ARRAY_FOREACH(p, effects) delete(*p);

    delete(transform);
    tvg::free(name);
}


void LottieLayer::prepare(RGB24* color)
{
    /* if layer is hidden, only useful data is its transform matrix.
       so force it to be a Null Layer and release all resource. */
    if (hidden) {
        type = LottieLayer::Null;
        ARRAY_FOREACH(p, children) delete(*p);
        children.reset();
        return;
    }

    //prepare the viewport clipper
    if (type == LottieLayer::Precomp) {
        auto clipper = Shape::gen();
        clipper->appendRect(0.0f, 0.0f, w, h);
        clipper->ref();
        statical.pooler.push(clipper);
    //prepare solid fill in advance if it is a layer type.
    } else if (color && type == LottieLayer::Solid) {
        auto solidFill = Shape::gen();
        solidFill->appendRect(0, 0, static_cast<float>(w), static_cast<float>(h));
        solidFill->fill(color->rgb[0], color->rgb[1], color->rgb[2]);
        solidFill->ref();
        statical.pooler.push(solidFill);
    }

    LottieGroup::prepare(LottieObject::Layer);
}


float LottieLayer::remap(LottieComposition* comp, float frameNo, LottieExpressions* exp)
{
    if (timeRemap.frames || timeRemap.value) {
        frameNo = comp->frameAtTime(timeRemap(frameNo, exp));
    } else {
        frameNo -= startFrame;
    }
    return (frameNo / timeStretch);
}


LottieComposition::~LottieComposition()
{
    if (!initiated && root) delete(root->scene);

    delete(root);
    tvg::free(version);
    tvg::free(name);

    ARRAY_FOREACH(p, interpolators) {
        tvg::free((*p)->key);
        tvg::free(*p);
    }

    ARRAY_FOREACH(p, assets) delete(*p);
    ARRAY_FOREACH(p, fonts) delete(*p);
    ARRAY_FOREACH(p, slots) delete(*p);
    ARRAY_FOREACH(p, markers) delete(*p);
}
