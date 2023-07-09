#include <jni.h>
#include <iostream>
#include <mpegfile.h>
#include <tstring.h>
#include <id3v2tag.h>
#include <id3v2frame.h>
#include <id3v2header.h>
#include <attachedpictureframe.h>
#include "fenrir_native.h"

using namespace std;
using namespace TagLib;

static string fixCov(const char *str) {
    string v = str;
    for (auto &i: v) {
        if (i == L'\"') {
            i = L'\'';
        }
    }
    return v;
}


extern "C" JNIEXPORT jboolean
Java_dev_ragnarok_fenrir_module_FileUtils_audioTagModifyNative(JNIEnv *env, jobject,
                                                               jstring audio_file,
                                                               jstring cover_file,
                                                               jstring cover_mime_type,
                                                               jstring title, jstring artist,
                                                               jstring album_title, jstring ifGenre,
                                                               jstring comment) {
    char const *audio_fileString = SafeGetStringUTFChars(env, audio_file, nullptr);
    if (!audio_fileString) {
        return false;
    }
    MPEG::File f(audio_fileString);
    if (!f.isOpen() || !f.isValid()) {
        env->ReleaseStringUTFChars(audio_file, audio_fileString);
        return false;
    }
    env->ReleaseStringUTFChars(audio_file, audio_fileString);
    auto v = f.ID3v2Tag(true);
    v->removeFrames("APIC");

    char const *titleString = SafeGetStringUTFChars(env, title, nullptr);
    if (titleString) {
        if (strlen(titleString) > 0) {
            v->setTitle(String(fixCov(titleString), String::Type::UTF8));
        }
        env->ReleaseStringUTFChars(title, titleString);
    }

    char const *artistString = SafeGetStringUTFChars(env, artist, nullptr);
    if (artistString) {
        if (strlen(artistString) > 0) {
            v->setArtist(String(fixCov(artistString), String::Type::UTF8));
        }
        env->ReleaseStringUTFChars(artist, artistString);
    }

    char const *album_titleString = SafeGetStringUTFChars(env, album_title, nullptr);
    if (album_titleString) {
        if (strlen(album_titleString) > 0) {
            v->setAlbum(String(fixCov(album_titleString), String::Type::UTF8));
        }
        env->ReleaseStringUTFChars(album_title, album_titleString);
    }

    char const *commentString = SafeGetStringUTFChars(env, comment, nullptr);
    if (commentString) {
        if (strlen(commentString) > 0) {
            v->setComment(String(fixCov(commentString), String::Type::UTF8));
        }
        env->ReleaseStringUTFChars(comment, commentString);
    }

    if (v->genre().isEmpty()) {
        char const *ifGenreString = SafeGetStringUTFChars(env, ifGenre, nullptr);
        if (ifGenreString) {
            if (strlen(ifGenreString) > 0) {
                v->setGenre(String(ifGenreString, String::Type::Latin1));
            }
            env->ReleaseStringUTFChars(ifGenre, ifGenreString);
        }
    }
    auto pic = new ID3v2::AttachedPictureFrame();
    ByteVector buf;
    char const *cover_fileString = SafeGetStringUTFChars(env, cover_file, nullptr);
    if (cover_fileString) {
        FILE *cv = fopen(cover_fileString, "rb");
        if (cv != nullptr) {
            char const *cover_mime_typeString = SafeGetStringUTFChars(env, cover_mime_type,
                                                                      nullptr);
            if (!cover_mime_typeString || strlen(cover_mime_typeString) <= 0) {
                pic->setMimeType("image/jpg");
            } else {
                pic->setMimeType(cover_mime_typeString);
                env->ReleaseStringUTFChars(cover_mime_type, cover_mime_typeString);
            }
            fseek(cv, 0, SEEK_END);
            int size = (int) ftell(cv);
            buf.resize(size, '\0');
            fseek(cv, 0, SEEK_SET);
            fread(buf.data(), 1, size, cv);
            fclose(cv);

            pic->setTextEncoding(String::Type::Latin1);
            pic->setType(ID3v2::AttachedPictureFrame::FrontCover);
            pic->setDescription("VK Cover");
            pic->setPicture(buf);
            v->addFrame(pic);
        }
        env->ReleaseStringUTFChars(cover_file, cover_fileString);
    }
    buf.clear();
    return f.save(MPEG::File::TagTypes::ID3v2, TagLib::File::StripOthers, ID3v2::v4, TagLib::File::Duplicate);
}

extern "C" JNIEXPORT jboolean
Java_dev_ragnarok_fenrir_module_FileUtils_audioTagStripNative(JNIEnv *env, jobject,
                                                              jstring audio_file) {
    char const *audio_fileString = SafeGetStringUTFChars(env, audio_file, nullptr);
    if (!audio_fileString) {
        return false;
    }
    MPEG::File f(audio_fileString);
    if (!f.isOpen() || !f.isValid()) {
        env->ReleaseStringUTFChars(audio_file, audio_fileString);
        return false;
    }
    env->ReleaseStringUTFChars(audio_file, audio_fileString);
    return f.strip(MPEG::File::AllTags);
}
