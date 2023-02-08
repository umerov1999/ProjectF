cmake_minimum_required(VERSION 3.10.2 FATAL_ERROR)
project(fenrir_jni C CXX ASM)

if (${CMAKE_BUILD_TYPE} STREQUAL "Debug")
    set(LTO_LINK "")
    set(CMAKE_CXX_FLAGS "-std=c++2a -DANDROID -g -DDEBUG_PLATFORM")
    set(CMAKE_C_FLAGS "-w -DANDROID -D_LARGEFILE_SOURCE=1 -g -DDEBUG_PLATFORM")
    set(OPTIMIZE_NORMAL "-O0")
    set(OPTIMIZE_FAST "-O0")
    set(SYM_VISIBILITY "")
else ()
    #set(LTO_LINK "-flto=thin")
    set(LTO_LINK "")
    set(CMAKE_CXX_FLAGS "-std=c++2a -DANDROID -ffunction-sections -fdata-sections -fmerge-all-constants ${LTO_LINK}")
    set(CMAKE_C_FLAGS "-w -DANDROID -D_LARGEFILE_SOURCE=1 -ffunction-sections -fdata-sections -fmerge-all-constants ${LTO_LINK}")
    set(OPTIMIZE_NORMAL "-Os")
    set(OPTIMIZE_FAST "-O3")
    set(SYM_VISIBILITY "-fvisibility=hidden")
endif ()
set(CMAKE_ASM_FLAGS "${CFLAGS} -x assembler-with-cpp")
set(CMAKE_CXX_STANDARD 20)

set(ffmpeg_location "$ENV{HOME}/ffmpeg")
set(ffmpeg_binaries "${ffmpeg_location}/android-libs/${ANDROID_ABI}")

foreach (ffmpeg_lib avutil swresample avcodec avformat swscale)
    set(ffmpeg_lib_filename lib${ffmpeg_lib}.a)
    set(ffmpeg_lib_file_path ${ffmpeg_binaries}/${ffmpeg_lib_filename})
    add_library(
            ${ffmpeg_lib}
            STATIC
            IMPORTED)
    set_target_properties(
            ${ffmpeg_lib} PROPERTIES
            IMPORTED_LOCATION
            ${ffmpeg_lib_file_path})
endforeach ()

add_library(thorvg STATIC
        thorvg/src/lib/sw_engine/tvgSwFill.cpp
        thorvg/src/lib/sw_engine/tvgSwImage.cpp
        thorvg/src/lib/sw_engine/tvgSwMath.cpp
        thorvg/src/lib/sw_engine/tvgSwMemPool.cpp
        thorvg/src/lib/sw_engine/tvgSwRaster.cpp
        thorvg/src/lib/sw_engine/tvgSwRenderer.cpp
        thorvg/src/lib/sw_engine/tvgSwRle.cpp
        thorvg/src/lib/sw_engine/tvgSwShape.cpp
        thorvg/src/lib/sw_engine/tvgSwStroke.cpp
        thorvg/src/lib/tvgAccessor.cpp
        thorvg/src/lib/tvgBezier.cpp
        thorvg/src/lib/tvgCanvas.cpp
        thorvg/src/lib/tvgFill.cpp
        thorvg/src/lib/tvgGlCanvas.cpp
        thorvg/src/lib/tvgInitializer.cpp
        thorvg/src/lib/tvgLinearGradient.cpp
        thorvg/src/lib/tvgLoader.cpp
        thorvg/src/lib/tvgPaint.cpp
        thorvg/src/lib/tvgPicture.cpp
        thorvg/src/lib/tvgRadialGradient.cpp
        thorvg/src/lib/tvgRender.cpp
        thorvg/src/lib/tvgSaver.cpp
        thorvg/src/lib/tvgScene.cpp
        thorvg/src/lib/tvgShape.cpp
        thorvg/src/lib/tvgSwCanvas.cpp
        thorvg/src/lib/tvgTaskScheduler.cpp
        thorvg/src/loaders/raw/tvgRawLoader.cpp
        thorvg/src/loaders/svg/tvgSvgCssStyle.cpp
        thorvg/src/loaders/svg/tvgSvgLoader.cpp
        thorvg/src/loaders/svg/tvgSvgPath.cpp
        thorvg/src/loaders/svg/tvgSvgSceneBuilder.cpp
        thorvg/src/loaders/svg/tvgSvgUtil.cpp
        thorvg/src/loaders/svg/tvgXmlParser.cpp)

target_compile_options(thorvg PRIVATE
        -fno-exceptions -ffast-math ${OPTIMIZE_NORMAL} -fno-rtti -fno-unwind-tables -fno-asynchronous-unwind-tables -Woverloaded-virtual -Wno-unused-parameter ${SYM_VISIBILITY})
target_include_directories(thorvg PRIVATE
        thorvg/inc
        thorvg/src/lib
        thorvg/src/lib/sw_engine
        thorvg/src/loaders/raw
        thorvg/src/loaders/svg)

add_library(rlottie STATIC
        rlottie/src/lottie/lottieanimation.cpp
        rlottie/src/lottie/lottieitem.cpp
        rlottie/src/lottie/lottiekeypath.cpp
        rlottie/src/lottie/lottieloader.cpp
        rlottie/src/lottie/lottiemodel.cpp
        rlottie/src/lottie/lottieparser.cpp
        rlottie/src/lottie/lottieitem_capi.cpp
        rlottie/src/vector/freetype/v_ft_math.cpp
        rlottie/src/vector/freetype/v_ft_raster.cpp
        rlottie/src/vector/freetype/v_ft_stroker.cpp
        rlottie/src/vector/stb/stb_image.cpp
        rlottie/src/vector/vbezier.cpp
        rlottie/src/vector/vbitmap.cpp
        rlottie/src/vector/vbrush.cpp
        rlottie/src/vector/varenaalloc.cpp
        rlottie/src/vector/vdasher.cpp
        rlottie/src/vector/vdebug.cpp
        rlottie/src/vector/vdrawable.cpp
        rlottie/src/vector/vdrawhelper.cpp
        rlottie/src/vector/vdrawhelper_sse2.cpp
        rlottie/src/vector/vdrawhelper_neon.cpp
        rlottie/src/vector/vdrawhelper_common.cpp
        rlottie/src/vector/velapsedtimer.cpp
        rlottie/src/vector/vimageloader.cpp
        rlottie/src/vector/vinterpolator.cpp
        rlottie/src/vector/vmatrix.cpp
        rlottie/src/vector/vpainter.cpp
        rlottie/src/vector/vpath.cpp
        rlottie/src/vector/vpathmesure.cpp
        rlottie/src/vector/vraster.cpp
        rlottie/src/vector/vrect.cpp
        rlottie/src/vector/vrle.cpp)
target_compile_options(rlottie PRIVATE
        -fno-exceptions -ffast-math ${OPTIMIZE_NORMAL} -fno-rtti -fno-unwind-tables -fno-asynchronous-unwind-tables -Woverloaded-virtual -Wno-unused-parameter ${SYM_VISIBILITY})
target_include_directories(rlottie PRIVATE
        rlottie/inc
        rlottie/src/vector
        rlottie/src/vector/pixman
        rlottie/src/vector/freetype
        rlottie/src/vector/stb)

add_library(libyuv STATIC
        animation/libyuv/source/compare_common.cc
        animation/libyuv/source/compare_gcc.cc
        animation/libyuv/source/compare_neon64.cc
        animation/libyuv/source/compare_win.cc
        animation/libyuv/source/compare.cc
        animation/libyuv/source/convert_argb.cc
        animation/libyuv/source/convert_from_argb.cc
        animation/libyuv/source/convert_from.cc
        animation/libyuv/source/convert_jpeg.cc
        animation/libyuv/source/convert_to_argb.cc
        animation/libyuv/source/convert_to_i420.cc
        animation/libyuv/source/convert.cc
        animation/libyuv/source/cpu_id.cc
        animation/libyuv/source/mjpeg_decoder.cc
        animation/libyuv/source/mjpeg_validate.cc
        animation/libyuv/source/planar_functions.cc
        animation/libyuv/source/rotate_any.cc
        animation/libyuv/source/rotate_argb.cc
        animation/libyuv/source/rotate_lsx.cc
        animation/libyuv/source/rotate_common.cc
        animation/libyuv/source/rotate_gcc.cc
        animation/libyuv/source/rotate_neon64.cc
        animation/libyuv/source/rotate_win.cc
        animation/libyuv/source/rotate.cc
        animation/libyuv/source/row_any.cc
        animation/libyuv/source/row_lasx.cc
        animation/libyuv/source/row_lsx.cc
        animation/libyuv/source/row_common.cc
        animation/libyuv/source/row_gcc.cc
        animation/libyuv/source/row_neon64.cc
        animation/libyuv/source/row_win.cc
        animation/libyuv/source/scale_any.cc
        animation/libyuv/source/scale_argb.cc
        animation/libyuv/source/scale_common.cc
        animation/libyuv/source/scale_lsx.cc
        animation/libyuv/source/scale_gcc.cc
        animation/libyuv/source/scale_neon64.cc
        animation/libyuv/source/scale_win.cc
        animation/libyuv/source/scale.cc
        animation/libyuv/source/video_common.cc
        animation/libyuv/source/scale_rgb.cc
        animation/libyuv/source/scale_uv.cc)
target_compile_options(libyuv PRIVATE
        -ffast-math ${OPTIMIZE_NORMAL} -funroll-loops -fno-strict-aliasing -fno-math-errno ${SYM_VISIBILITY})
target_include_directories(libyuv PRIVATE
        animation/libyuv/include)

if (${ANDROID_ABI} STREQUAL "armeabi-v7a")
    set_target_properties(libyuv PROPERTIES
            ANDROID_ARM_NEON FALSE
            ANDROID_ARM_MODE arm)
    target_compile_definitions(libyuv PRIVATE
            ANDROID_ARM_NEON=false)
    target_sources(libyuv PRIVATE
            animation/libyuv/source/compare_neon.cc
            animation/libyuv/source/rotate_neon.cc
            animation/libyuv/source/row_neon.cc
            animation/libyuv/source/scale_neon.cc)
else ()
    set_target_properties(libyuv PROPERTIES
            ANDROID_ARM_NEON FALSE
            ANDROID_ARM_MODE arm)
endif ()

add_library(opus STATIC
        audio/opus/libopus/src/opus.c
        audio/opus/libopus/src/opus_decoder.c
        audio/opus/libopus/src/opus_encoder.c
        audio/opus/libopus/src/opus_multistream.c
        audio/opus/libopus/src/opus_multistream_encoder.c
        audio/opus/libopus/src/opus_multistream_decoder.c
        audio/opus/libopus/src/repacketizer.c
        audio/opus/libopus/src/analysis.c
        audio/opus/libopus/src/mlp.c
        audio/opus/libopus/src/mlp_data.c
        audio/opus/libopus/src/opus_projection_encoder.c
        audio/opus/libopus/src/opus_projection_decoder.c
        audio/opus/libopus/src/mapping_matrix.c
        audio/opus/libopus/silk/CNG.c
        audio/opus/libopus/silk/code_signs.c
        audio/opus/libopus/silk/init_decoder.c
        audio/opus/libopus/silk/decode_core.c
        audio/opus/libopus/silk/decode_frame.c
        audio/opus/libopus/silk/decode_parameters.c
        audio/opus/libopus/silk/decode_indices.c
        audio/opus/libopus/silk/decode_pulses.c
        audio/opus/libopus/silk/decoder_set_fs.c
        audio/opus/libopus/silk/dec_API.c
        audio/opus/libopus/silk/enc_API.c
        audio/opus/libopus/silk/encode_indices.c
        audio/opus/libopus/silk/encode_pulses.c
        audio/opus/libopus/silk/gain_quant.c
        audio/opus/libopus/silk/interpolate.c
        audio/opus/libopus/silk/LP_variable_cutoff.c
        audio/opus/libopus/silk/NLSF_decode.c
        audio/opus/libopus/silk/NSQ.c
        audio/opus/libopus/silk/NSQ_del_dec.c
        audio/opus/libopus/silk/PLC.c
        audio/opus/libopus/silk/shell_coder.c
        audio/opus/libopus/silk/tables_gain.c
        audio/opus/libopus/silk/tables_LTP.c
        audio/opus/libopus/silk/tables_NLSF_CB_NB_MB.c
        audio/opus/libopus/silk/tables_NLSF_CB_WB.c
        audio/opus/libopus/silk/tables_other.c
        audio/opus/libopus/silk/tables_pitch_lag.c
        audio/opus/libopus/silk/tables_pulses_per_block.c
        audio/opus/libopus/silk/VAD.c
        audio/opus/libopus/silk/control_audio_bandwidth.c
        audio/opus/libopus/silk/quant_LTP_gains.c
        audio/opus/libopus/silk/VQ_WMat_EC.c
        audio/opus/libopus/silk/HP_variable_cutoff.c
        audio/opus/libopus/silk/NLSF_encode.c
        audio/opus/libopus/silk/NLSF_VQ.c
        audio/opus/libopus/silk/NLSF_unpack.c
        audio/opus/libopus/silk/NLSF_del_dec_quant.c
        audio/opus/libopus/silk/process_NLSFs.c
        audio/opus/libopus/silk/stereo_LR_to_MS.c
        audio/opus/libopus/silk/stereo_MS_to_LR.c
        audio/opus/libopus/silk/check_control_input.c
        audio/opus/libopus/silk/control_SNR.c
        audio/opus/libopus/silk/init_encoder.c
        audio/opus/libopus/silk/control_codec.c
        audio/opus/libopus/silk/A2NLSF.c
        audio/opus/libopus/silk/ana_filt_bank_1.c
        audio/opus/libopus/silk/biquad_alt.c
        audio/opus/libopus/silk/bwexpander_32.c
        audio/opus/libopus/silk/bwexpander.c
        audio/opus/libopus/silk/debug.c
        audio/opus/libopus/silk/decode_pitch.c
        audio/opus/libopus/silk/inner_prod_aligned.c
        audio/opus/libopus/silk/lin2log.c
        audio/opus/libopus/silk/log2lin.c
        audio/opus/libopus/silk/LPC_analysis_filter.c
        audio/opus/libopus/silk/LPC_inv_pred_gain.c
        audio/opus/libopus/silk/table_LSF_cos.c
        audio/opus/libopus/silk/NLSF2A.c
        audio/opus/libopus/silk/NLSF_stabilize.c
        audio/opus/libopus/silk/NLSF_VQ_weights_laroia.c
        audio/opus/libopus/silk/pitch_est_tables.c
        audio/opus/libopus/silk/resampler.c
        audio/opus/libopus/silk/resampler_down2_3.c
        audio/opus/libopus/silk/resampler_down2.c
        audio/opus/libopus/silk/resampler_private_AR2.c
        audio/opus/libopus/silk/resampler_private_down_FIR.c
        audio/opus/libopus/silk/resampler_private_IIR_FIR.c
        audio/opus/libopus/silk/resampler_private_up2_HQ.c
        audio/opus/libopus/silk/resampler_rom.c
        audio/opus/libopus/silk/sigm_Q15.c
        audio/opus/libopus/silk/sort.c
        audio/opus/libopus/silk/sum_sqr_shift.c
        audio/opus/libopus/silk/stereo_decode_pred.c
        audio/opus/libopus/silk/stereo_encode_pred.c
        audio/opus/libopus/silk/stereo_find_predictor.c
        audio/opus/libopus/silk/stereo_quant_pred.c
        audio/opus/libopus/silk/LPC_fit.c
        audio/opus/libopus/silk/fixed/LTP_analysis_filter_FIX.c
        audio/opus/libopus/silk/fixed/LTP_scale_ctrl_FIX.c
        audio/opus/libopus/silk/fixed/corrMatrix_FIX.c
        audio/opus/libopus/silk/fixed/encode_frame_FIX.c
        audio/opus/libopus/silk/fixed/find_LPC_FIX.c
        audio/opus/libopus/silk/fixed/find_LTP_FIX.c
        audio/opus/libopus/silk/fixed/find_pitch_lags_FIX.c
        audio/opus/libopus/silk/fixed/find_pred_coefs_FIX.c
        audio/opus/libopus/silk/fixed/noise_shape_analysis_FIX.c
        audio/opus/libopus/silk/fixed/process_gains_FIX.c
        audio/opus/libopus/silk/fixed/regularize_correlations_FIX.c
        audio/opus/libopus/silk/fixed/residual_energy16_FIX.c
        audio/opus/libopus/silk/fixed/residual_energy_FIX.c
        audio/opus/libopus/silk/fixed/warped_autocorrelation_FIX.c
        audio/opus/libopus/silk/fixed/apply_sine_window_FIX.c
        audio/opus/libopus/silk/fixed/autocorr_FIX.c
        audio/opus/libopus/silk/fixed/burg_modified_FIX.c
        audio/opus/libopus/silk/fixed/k2a_FIX.c
        audio/opus/libopus/silk/fixed/k2a_Q16_FIX.c
        audio/opus/libopus/silk/fixed/pitch_analysis_core_FIX.c
        audio/opus/libopus/silk/fixed/vector_ops_FIX.c
        audio/opus/libopus/silk/fixed/schur64_FIX.c
        audio/opus/libopus/silk/fixed/schur_FIX.c
        audio/opus/libopus/celt/bands.c
        audio/opus/libopus/celt/celt.c
        audio/opus/libopus/celt/celt_encoder.c
        audio/opus/libopus/celt/celt_decoder.c
        audio/opus/libopus/celt/cwrs.c
        audio/opus/libopus/celt/entcode.c
        audio/opus/libopus/celt/entdec.c
        audio/opus/libopus/celt/entenc.c
        audio/opus/libopus/celt/kiss_fft.c
        audio/opus/libopus/celt/laplace.c
        audio/opus/libopus/celt/mathops.c
        audio/opus/libopus/celt/mdct.c
        audio/opus/libopus/celt/modes.c
        audio/opus/libopus/celt/pitch.c
        audio/opus/libopus/celt/celt_lpc.c
        audio/opus/libopus/celt/quant_bands.c
        audio/opus/libopus/celt/rate.c
        audio/opus/libopus/celt/vq.c
        audio/opus/libopus/celt/arm/armcpu.c
        audio/opus/libopus/celt/arm/arm_celt_map.c
        )
if (${ANDROID_ABI} STREQUAL "armeabi-v7a" OR ${ANDROID_ABI} STREQUAL "arm64-v8a")
    target_compile_definitions(opus PRIVATE
            LIBYUV_NEON OPUS_HAVE_RTCD OPUS_ARM_ASM)

    target_sources(opus PRIVATE
            audio/opus/libopus/celt/arm/celt_neon_intr.c
            audio/opus/libopus/celt/arm/pitch_neon_intr.c
            audio/opus/libopus/silk/arm/NSQ_neon.c
            audio/opus/libopus/silk/arm/arm_silk_map.c
            audio/opus/libopus/silk/arm/LPC_inv_pred_gain_neon_intr.c
            audio/opus/libopus/silk/arm/NSQ_del_dec_neon_intr.c
            audio/opus/libopus/silk/arm/biquad_alt_neon_intr.c
            audio/opus/libopus/silk/fixed/arm/warped_autocorrelation_FIX_neon_intr.c
            )
endif ()
target_include_directories(opus PRIVATE
        audio/opus/libopus/include
        audio/opus/libopus/silk
        audio/opus/libopus/silk/fixed
        audio/opus/libopus/celt
        audio/opus/libopus)
target_compile_options(opus PRIVATE
        -ffast-math ${OPTIMIZE_NORMAL} -funroll-loops -ffast-math -fno-strict-aliasing -fno-math-errno ${SYM_VISIBILITY})
set_target_properties(opus PROPERTIES
        ANDROID_ARM_MODE arm)

target_compile_definitions(opus PRIVATE
        OPUS_BUILD FIXED_POINT USE_ALLOCA restrict= __EMX__ HAVE_LRINT HAVE_LRINTF)

add_library(common STATIC
        audio/ts/MpegTsBitStream.cpp
        audio/ts/MpegTsDemux.cpp
        audio/taglib/ape/apefile.cpp
        audio/taglib/ape/apefooter.cpp
        audio/taglib/ape/apeitem.cpp
        audio/taglib/ape/apeproperties.cpp
        audio/taglib/ape/apetag.cpp
        audio/taglib/audioproperties.cpp
        audio/taglib/fileref.cpp
        audio/taglib/mpeg/id3v1/id3v1genres.cpp
        audio/taglib/mpeg/id3v1/id3v1tag.cpp
        audio/taglib/mpeg/id3v2/frames/attachedpictureframe.cpp
        audio/taglib/mpeg/id3v2/frames/chapterframe.cpp
        audio/taglib/mpeg/id3v2/frames/commentsframe.cpp
        audio/taglib/mpeg/id3v2/frames/eventtimingcodesframe.cpp
        audio/taglib/mpeg/id3v2/frames/generalencapsulatedobjectframe.cpp
        audio/taglib/mpeg/id3v2/frames/ownershipframe.cpp
        audio/taglib/mpeg/id3v2/frames/podcastframe.cpp
        audio/taglib/mpeg/id3v2/frames/popularimeterframe.cpp
        audio/taglib/mpeg/id3v2/frames/privateframe.cpp
        audio/taglib/mpeg/id3v2/frames/relativevolumeframe.cpp
        audio/taglib/mpeg/id3v2/frames/synchronizedlyricsframe.cpp
        audio/taglib/mpeg/id3v2/frames/tableofcontentsframe.cpp
        audio/taglib/mpeg/id3v2/frames/textidentificationframe.cpp
        audio/taglib/mpeg/id3v2/frames/uniquefileidentifierframe.cpp
        audio/taglib/mpeg/id3v2/frames/unknownframe.cpp
        audio/taglib/mpeg/id3v2/frames/unsynchronizedlyricsframe.cpp
        audio/taglib/mpeg/id3v2/frames/urllinkframe.cpp
        audio/taglib/mpeg/id3v2/id3v2extendedheader.cpp
        audio/taglib/mpeg/id3v2/id3v2footer.cpp
        audio/taglib/mpeg/id3v2/id3v2frame.cpp
        audio/taglib/mpeg/id3v2/id3v2framefactory.cpp
        audio/taglib/mpeg/id3v2/id3v2header.cpp
        audio/taglib/mpeg/id3v2/id3v2synchdata.cpp
        audio/taglib/mpeg/id3v2/id3v2tag.cpp
        audio/taglib/mpeg/mpegfile.cpp
        audio/taglib/mpeg/mpegheader.cpp
        audio/taglib/mpeg/mpegproperties.cpp
        audio/taglib/mpeg/xingheader.cpp
        audio/taglib/tag.cpp
        audio/taglib/tagunion.cpp
        audio/taglib/tagutils.cpp
        audio/taglib/toolkit/tbytevector.cpp
        audio/taglib/toolkit/tbytevectorlist.cpp
        audio/taglib/toolkit/tbytevectorstream.cpp
        audio/taglib/toolkit/tdebug.cpp
        audio/taglib/toolkit/tdebuglistener.cpp
        audio/taglib/toolkit/tfile.cpp
        audio/taglib/toolkit/tfilestream.cpp
        audio/taglib/toolkit/tiostream.cpp
        audio/taglib/toolkit/tpropertymap.cpp
        audio/taglib/toolkit/trefcounter.cpp
        audio/taglib/toolkit/tstring.cpp
        audio/taglib/toolkit/tstringlist.cpp
        audio/taglib/toolkit/tzlib.cpp
        compress/lz4/lz4.c
        compress/zlib/adler32.c
        compress/zlib/compress.c
        compress/zlib/crc32.c
        compress/zlib/deflate.c
        compress/zlib/gzclose.c
        compress/zlib/gzlib.c
        compress/zlib/gzread.c
        compress/zlib/gzwrite.c
        compress/zlib/infback.c
        compress/zlib/inffast.c
        compress/zlib/inflate.c
        compress/zlib/inftrees.c
        compress/zlib/trees.c
        compress/zlib/uncompr.c
        compress/zlib/zutil.c
        compress/zstd/legacy/zstd_v06.c
        compress/zstd/legacy/zstd_v05.c
        compress/zstd/legacy/zstd_v07.c
        compress/zstd/legacy/zstd_v04.c
        compress/zstd/dictBuilder/fastcover.c
        compress/zstd/dictBuilder/cover.c
        compress/zstd/dictBuilder/zdict.c
        compress/zstd/dictBuilder/divsufsort.c
        compress/zstd/decompress/huf_decompress.c
        compress/zstd/decompress/zstd_decompress.c
        compress/zstd/decompress/zstd_ddict.c
        compress/zstd/decompress/zstd_decompress_block.c
        compress/zstd/jni_zdict.c
        compress/zstd/common/xxhash.c
        compress/zstd/common/error_private.c
        compress/zstd/common/pool.c
        compress/zstd/common/fse_decompress.c
        compress/zstd/common/threading.c
        compress/zstd/common/debug.c
        compress/zstd/common/zstd_common.c
        compress/zstd/common/entropy_common.c
        compress/zstd/compress/zstdmt_compress.c
        compress/zstd/compress/zstd_fast.c
        compress/zstd/compress/zstd_compress_sequences.c
        compress/zstd/compress/zstd_lazy.c
        compress/zstd/compress/zstd_compress_literals.c
        compress/zstd/compress/hist.c
        compress/zstd/compress/zstd_double_fast.c
        compress/zstd/compress/huf_compress.c
        compress/zstd/compress/zstd_ldm.c
        compress/zstd/compress/zstd_opt.c
        compress/zstd/compress/zstd_compress_superblock.c
        compress/zstd/decompress/huf_decompress_amd64.S
        compress/zstd/compress/fse_compress.c
        compress/zstd/compress/zstd_compress.c
        )

target_include_directories(common
        PRIVATE
        ./
        audio/taglib
        audio/taglib/mpeg
        audio/taglib/mpeg/id3v1
        audio/taglib/mpeg/id3v2
        audio/taglib/mpeg/id3v2/frames
        audio/taglib/toolkit
        audio/taglib/utf8
        audio/taglib/utf8/utf8
        audio/taglib/ape
        compress/lz4
        compress/zlib
        compress/zstd
        compress/zstd/common
        compress/zstd/compress
        compress/zstd/decompress
        compress/zstd/dictBuilder
        compress/zstd/legacy)

target_compile_options(common PRIVATE
        ${OPTIMIZE_FAST} -DZSTD_MULTITHREAD=1 -DZSTD_LEGACY_SUPPORT=4 -DHAVE_ZLIB=1 -DTAGLIB_STATIC=1 -funroll-loops -fno-strict-aliasing -fno-math-errno -Wno-unused-parameter -Wno-deprecated-declarations -Wno-unused-variable ${SYM_VISIBILITY})

add_library(fenrir_jni
        SHARED
        lottie_jni.cpp
        thorvg_jni.cpp
        audio/ffmpeg/ffmpeg_jni.cpp
        audio/opus/opus_jni.cc
        animation/animation_jni.cpp
        parcel_jni.cpp
        string_exist_jni.cpp
        file_utils_jni.cpp
        image_processing_util_jni.cpp
        audio/ffmpeg/to_mp4_audio_jni.cpp
        audio/ts/mpegts_demux_jni.cpp
        audio/taglib/taglib_jni.cpp
        compress/zstd/jni_bufferdecompress_zstd.c
        compress/zstd/jni_directbuffercompress_zstd.c
        compress/zstd/jni_directbufferdecompress_zstd.c
        compress/zstd/jni_fast_zstd.c
        compress/zstd/jni_inputstream_zstd.c
        compress/zstd/jni_outputstream_zstd.c
        compress/zstd/jni_zdict.c
        compress/zstd/jni_zstd.c)

target_include_directories(fenrir_jni
        PRIVATE
        ./
        rlottie/inc
        thorvg/inc
        animation/libyuv/include
        exoplayer/include
        audio/opus/libopus/include
        compress/lz4
        compress/zstd
        compress/zstd/common
        compress/zstd/compress
        compress/zstd/decompress
        compress/zstd/dictBuilder
        compress/zstd/legacy
        compress/zlib
        audio/taglib
        audio/taglib/mpeg
        audio/taglib/mpeg/id3v1
        audio/taglib/mpeg/id3v2
        audio/taglib/mpeg/id3v2/frames
        audio/taglib/toolkit
        animation/libyuv/include
        ${ffmpeg_location})

target_compile_options(fenrir_jni PRIVATE
        ${OPTIMIZE_NORMAL} -funroll-loops -fno-strict-aliasing -fno-math-errno -Wno-unused-parameter -Wno-deprecated-declarations -Wno-unused-variable)

target_link_libraries(fenrir_jni
        PRIVATE android
        PRIVATE jnigraphics
        PRIVATE log
        PRIVATE swscale ${LTO_LINK}
        PRIVATE avformat ${LTO_LINK}
        PRIVATE avcodec ${LTO_LINK}
        PRIVATE swresample ${LTO_LINK}
        PRIVATE avutil ${LTO_LINK}
        PRIVATE common ${LTO_LINK}
        PRIVATE rlottie ${LTO_LINK}
        PRIVATE thorvg ${LTO_LINK}
        PRIVATE libyuv ${LTO_LINK}
        PRIVATE opus ${LTO_LINK})
