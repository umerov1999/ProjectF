/***************************************************************************
    copyright            : (C) 2002 - 2008 by Scott Wheeler
    email                : wheeler@kde.org

    copyright            : (C) 2010 by Alex Novichkov
    email                : novichko@atnet.ru
                           (added APE file support)
 ***************************************************************************/

/***************************************************************************
 *   This library is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU Lesser General Public License version   *
 *   2.1 as published by the Free Software Foundation.                     *
 *                                                                         *
 *   This library is distributed in the hope that it will be useful, but   *
 *   WITHOUT ANY WARRANTY; without even the implied warranty of            *
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU     *
 *   Lesser General Public License for more details.                       *
 *                                                                         *
 *   You should have received a copy of the GNU Lesser General Public      *
 *   License along with this library; if not, write to the Free Software   *
 *   Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA         *
 *   02110-1301  USA                                                       *
 *                                                                         *
 *   Alternatively, this file is available under the Mozilla Public        *
 *   License Version 1.1.  You may obtain a copy of the License at         *
 *   http://www.mozilla.org/MPL/                                           *
 ***************************************************************************/

#include "fileref.h"

#include <cstring>

#include "tdebug.h"
#include "tfile.h"
#include "tfilestream.h"
#include "tstring.h"

#include "mpegfile.h"
#include "id3v2framefactory.h"
#include "apefile.h"

using namespace TagLib;

class FileRef::FileTypeResolver::FileTypeResolverPrivate
{
};

class FileRef::StreamTypeResolver::StreamTypeResolverPrivate
{
};

namespace
{
  typedef List<const FileRef::FileTypeResolver *> ResolverList;
  ResolverList fileTypeResolvers;

  // Detect the file type by user-defined resolvers.

  File *detectByResolvers(FileName fileName, bool readAudioProperties,
                          AudioProperties::ReadStyle audioPropertiesStyle)
  {
#ifdef _WIN32
    if(::wcslen(fileName) == 0)
      return 0;
#else
    if(::strlen(fileName) == 0)
      return nullptr;
#endif
    for(auto it = fileTypeResolvers.cbegin(); it != fileTypeResolvers.cend(); ++it) {
      File *file = (*it)->createFile(fileName, readAudioProperties, audioPropertiesStyle);
      if(file)
        return file;
    }

    return nullptr;
  }

  File *detectByResolvers(IOStream* stream, bool readAudioProperties,
                          AudioProperties::ReadStyle audioPropertiesStyle)
  {
    for(auto it = fileTypeResolvers.cbegin(); it != fileTypeResolvers.cend(); ++it) {
      if(auto streamResolver = dynamic_cast<const FileRef::StreamTypeResolver*>(*it)) {
        if(File *file = streamResolver->createFileFromStream(
             stream, readAudioProperties, audioPropertiesStyle))
          return file;
      }
    }

    return nullptr;
  }

  // Detect the file type based on the file extension.

  File* detectByExtension(IOStream *stream, bool readAudioProperties,
                          AudioProperties::ReadStyle audioPropertiesStyle)
  {
#ifdef _WIN32
    const String s = stream->name().toString();
#else
    const String s(stream->name());
#endif

    String ext;
    const int pos = s.rfind(".");
    if(pos != -1)
      ext = s.substr(pos + 1).upper();

    // If this list is updated, the method defaultFileExtensions() should also be
    // updated.  However at some point that list should be created at the same time
    // that a default file type resolver is created.

    if(ext.isEmpty())
      return nullptr;

    // .oga can be any audio in the Ogg container. So leave it to content-based detection.

    File *file = nullptr;

    if(ext == "MP3")
      file = new MPEG::File(stream, ID3v2::FrameFactory::instance(), readAudioProperties, audioPropertiesStyle);
    else if(ext == "APE")
      file = new APE::File(stream, readAudioProperties, audioPropertiesStyle);
    // module, nst and wow are possible but uncommon extensions

    // if file is not valid, leave it to content-based detection.

    if(file) {
      if(file->isValid())
        return file;
      delete file;
    }

    return nullptr;
  }

  // Detect the file type based on the actual content of the stream.

  File *detectByContent(IOStream *stream, bool readAudioProperties,
                        AudioProperties::ReadStyle audioPropertiesStyle)
  {
    File *file = nullptr;

    if(MPEG::File::isSupported(stream))
      file = new MPEG::File(stream, ID3v2::FrameFactory::instance(), readAudioProperties, audioPropertiesStyle);
    else if(APE::File::isSupported(stream))
      file = new APE::File(stream, readAudioProperties, audioPropertiesStyle);

    // isSupported() only does a quick check, so double check the file here.

    if(file) {
      if(file->isValid())
        return file;
      delete file;
    }

    return nullptr;
  }

  // Internal function that supports FileRef::create().
  // This looks redundant, but necessary in order not to change the previous
  // behavior of FileRef::create().

  File* createInternal(FileName fileName, bool readAudioProperties,
                       AudioProperties::ReadStyle audioPropertiesStyle)
  {
    File *file = detectByResolvers(fileName, readAudioProperties, audioPropertiesStyle);
    if(file)
      return file;

#ifdef _WIN32
    const String s = fileName.toString();
#else
    const String s(fileName);
#endif

    String ext;
    const int pos = s.rfind(".");
    if(pos != -1)
      ext = s.substr(pos + 1).upper();

    if(ext.isEmpty())
      return nullptr;

    if(ext == "MP3")
      return new MPEG::File(fileName, ID3v2::FrameFactory::instance(), readAudioProperties, audioPropertiesStyle);
    if(ext == "APE")
      return new APE::File(fileName, readAudioProperties, audioPropertiesStyle);
    // module, nst and wow are possible but uncommon extensions

    return nullptr;
  }
}  // namespace

class FileRef::FileRefPrivate
{
public:
  FileRefPrivate() :
    file(nullptr),
    stream(nullptr) {}

  ~FileRefPrivate()
  {
    delete file;
    delete stream;
  }

  FileRefPrivate(const FileRefPrivate &) = delete;
  FileRefPrivate &operator=(const FileRefPrivate &) = delete;

  File     *file;
  IOStream *stream;
};

////////////////////////////////////////////////////////////////////////////////
// public members
////////////////////////////////////////////////////////////////////////////////

FileRef::FileRef() :
  d(std::make_shared<FileRefPrivate>())
{
}

FileRef::FileRef(FileName fileName, bool readAudioProperties,
                 AudioProperties::ReadStyle audioPropertiesStyle) :
  d(std::make_shared<FileRefPrivate>())
{
  parse(fileName, readAudioProperties, audioPropertiesStyle);
}

FileRef::FileRef(IOStream *stream, bool readAudioProperties, AudioProperties::ReadStyle audioPropertiesStyle) :
  d(std::make_shared<FileRefPrivate>())
{
  parse(stream, readAudioProperties, audioPropertiesStyle);
}

FileRef::FileRef(File *file) :
  d(std::make_shared<FileRefPrivate>())
{
  d->file = file;
}

FileRef::FileRef(const FileRef &ref) = default;

FileRef::~FileRef() = default;

Tag *FileRef::tag() const
{
  if(isNull()) {
    debug("FileRef::tag() - Called without a valid file.");
    return nullptr;
  }
  return d->file->tag();
}

AudioProperties *FileRef::audioProperties() const
{
  if(isNull()) {
    debug("FileRef::audioProperties() - Called without a valid file.");
    return nullptr;
  }
  return d->file->audioProperties();
}

File *FileRef::file() const
{
  return d->file;
}

bool FileRef::save()
{
  if(isNull()) {
    debug("FileRef::save() - Called without a valid file.");
    return false;
  }
  return d->file->save();
}

const FileRef::FileTypeResolver *FileRef::addFileTypeResolver(const FileRef::FileTypeResolver *resolver) // static
{
  fileTypeResolvers.prepend(resolver);
  return resolver;
}

StringList FileRef::defaultFileExtensions()
{
  StringList l;

  l.append("ogg");
  l.append("flac");
  l.append("oga");
  l.append("opus");
  l.append("mp3");
  l.append("mpc");
  l.append("wv");
  l.append("spx");
  l.append("tta");
  l.append("m4a");
  l.append("m4r");
  l.append("m4b");
  l.append("m4p");
  l.append("3g2");
  l.append("mp4");
  l.append("m4v");
  l.append("wma");
  l.append("asf");
  l.append("aif");
  l.append("aiff");
  l.append("afc");
  l.append("aifc");
  l.append("wav");
  l.append("ape");
  l.append("mod");
  l.append("module"); // alias for "mod"
  l.append("nst"); // alias for "mod"
  l.append("wow"); // alias for "mod"
  l.append("s3m");
  l.append("it");
  l.append("xm");

  return l;
}

bool FileRef::isNull() const
{
  return (!d->file || !d->file->isValid());
}

FileRef &FileRef::operator=(const FileRef &ref)
{
  FileRef(ref).swap(*this);
  return *this;
}

void FileRef::swap(FileRef &ref)
{
  using std::swap;

  swap(d, ref.d);
}

bool FileRef::operator==(const FileRef &ref) const
{
  return (ref.d->file == d->file);
}

bool FileRef::operator!=(const FileRef &ref) const
{
  return (ref.d->file != d->file);
}

File *FileRef::create(FileName fileName, bool readAudioProperties,
                      AudioProperties::ReadStyle audioPropertiesStyle) // static
{
  return createInternal(fileName, readAudioProperties, audioPropertiesStyle);
}

////////////////////////////////////////////////////////////////////////////////
// private members
////////////////////////////////////////////////////////////////////////////////

void FileRef::parse(FileName fileName, bool readAudioProperties,
                    AudioProperties::ReadStyle audioPropertiesStyle)
{
  // Try user-defined resolvers.

  d->file = detectByResolvers(fileName, readAudioProperties, audioPropertiesStyle);
  if(d->file)
    return;

  // Try to resolve file types based on the file extension.

  d->stream = new FileStream(fileName);
  d->file = detectByExtension(d->stream, readAudioProperties, audioPropertiesStyle);
  if(d->file)
    return;

  // At last, try to resolve file types based on the actual content.

  d->file = detectByContent(d->stream, readAudioProperties, audioPropertiesStyle);
  if(d->file)
    return;

  // Stream have to be closed here if failed to resolve file types.

  delete d->stream;
  d->stream = nullptr;
}

void FileRef::parse(IOStream *stream, bool readAudioProperties,
                    AudioProperties::ReadStyle audioPropertiesStyle)
{
  // Try user-defined stream resolvers.

  d->file = detectByResolvers(stream, readAudioProperties, audioPropertiesStyle);
  if(d->file)
    return;

  // Try user-defined resolvers.

  d->file = detectByResolvers(stream->name(), readAudioProperties, audioPropertiesStyle);
  if(d->file)
    return;

  // Try to resolve file types based on the file extension.

  d->file = detectByExtension(stream, readAudioProperties, audioPropertiesStyle);
  if(d->file)
    return;

  // At last, try to resolve file types based on the actual content of the file.

  d->file = detectByContent(stream, readAudioProperties, audioPropertiesStyle);
}

FileRef::FileTypeResolver::FileTypeResolver() = default;
FileRef::FileTypeResolver::~FileTypeResolver() = default;

FileRef::StreamTypeResolver::StreamTypeResolver() = default;
FileRef::StreamTypeResolver::~StreamTypeResolver() = default;
