#define THORVG_VERSION_STRING "1.0.0"
#define THORVG_SW_RASTER_SUPPORT
#define THORVG_SVG_LOADER_SUPPORT
//#define THORVG_PNG_LOADER_SUPPORT
//#define THORVG_JPG_LOADER_SUPPORT
#define THORVG_LOTTIE_LOADER_SUPPORT
#define THORVG_THREAD_SUPPORT
#if defined(__ARM_NEON__) || defined(__aarch64__)
#define THORVG_NEON_VECTOR_SUPPORT
#else
//#define THORVG_AVX_VECTOR_SUPPORT
#endif
