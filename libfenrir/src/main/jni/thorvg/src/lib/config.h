#define THORVG_VERSION_STRING "0.10.99"
#define THORVG_SW_RASTER_SUPPORT
#define THORVG_SVG_LOADER_SUPPORT
#if defined(__ARM_NEON__) || defined(__aarch64__)
#define THORVG_NEON_VECTOR_SUPPORT
#else
//#define THORVG_AVX_VECTOR_SUPPORT
#endif
#define strdup strdup
