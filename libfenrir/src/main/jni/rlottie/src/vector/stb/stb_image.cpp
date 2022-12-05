/*
 * configure stb_image about
 * the image we will support
 */
#define STB_IMAGE_IMPLEMENTATION

#define STBI_ONLY_JPEG
#define STBI_ONLY_PNG
#define STBI_ONLY_BMP
#define STBI_ONLY_HDR
#define STBI_NO_PSD
#define STBI_ONLY_GIF
#define STBI_ONLY_PIC
#define STBI_ONLY_TGA
#define STBI_ONLY_ZLIB
#define STBI_ONLY_PNM

#include "stb_image.h"

#define RLOTTIE_API

#ifdef __cplusplus
extern "C" {
#endif

/*
 * exported function wrapper from the library
 */

RLOTTIE_API unsigned char *lottie_image_load(char const *filename, int *x,
                                            int *y, int *comp, int req_comp)
{
    return stbi_load(filename, x, y, comp, req_comp);
}

RLOTTIE_API unsigned char *lottie_image_load_from_data(const char *imageData,
                                                      int len, int *x, int *y,
                                                      int *comp, int req_comp)
{
    auto *data = (unsigned char *)imageData;
    return stbi_load_from_memory(data, len, x, y, comp, req_comp);
}

RLOTTIE_API void lottie_image_free(unsigned char *data)
{
    stbi_image_free(data);
}

#ifdef __cplusplus
}
#endif
