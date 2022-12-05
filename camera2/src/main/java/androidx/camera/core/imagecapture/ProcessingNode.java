/*
 * Copyright 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.camera.core.imagecapture;

import static android.graphics.ImageFormat.YUV_420_888;

import static androidx.camera.core.ImageCapture.ERROR_UNKNOWN;
import static androidx.camera.core.impl.utils.executor.CameraXExecutors.mainThreadExecutor;

import static java.util.Objects.requireNonNull;

import android.graphics.Bitmap;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.annotation.VisibleForTesting;
import androidx.annotation.WorkerThread;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.impl.utils.executor.CameraXExecutors;
import androidx.camera.core.processing.Edge;
import androidx.camera.core.processing.Node;
import androidx.camera.core.processing.Operation;
import androidx.camera.core.processing.Packet;

import com.google.auto.value.AutoValue;

import java.util.concurrent.Executor;

/**
 * Processes a single image and invokes {@link TakePictureCallback}.
 *
 * <p>This node performs operations that runs on a single image, such as cropping, format
 * conversion, effects and/or saving to disk.
 */
@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class ProcessingNode implements Node<ProcessingNode.In, Void> {

    @NonNull
    private final Executor mBlockingExecutor;

    private Operation<InputPacket, Packet<ImageProxy>> mInput2Packet;
    private Operation<Image2JpegBytes.In, Packet<byte[]>> mImage2JpegBytes;
    private Operation<Bitmap2JpegBytes.In, Packet<byte[]>> mBitmap2JpegBytes;
    private Operation<JpegBytes2Disk.In, ImageCapture.OutputFileResults> mJpegBytes2Disk;
    private Operation<Packet<byte[]>, Packet<Bitmap>> mJpegBytes2CroppedBitmap;
    private Operation<Packet<ImageProxy>, ImageProxy> mJpegImage2Result;
    private Operation<Packet<byte[]>, Packet<ImageProxy>> mJpegBytes2Image;

    /**
     * @param blockingExecutor a executor that can be blocked by long running tasks. e.g.
     *                         {@link CameraXExecutors#ioExecutor()}
     */
    ProcessingNode(@NonNull Executor blockingExecutor) {
        mBlockingExecutor = blockingExecutor;
    }

    @NonNull
    @Override
    public Void transform(@NonNull ProcessingNode.In inputEdge) {
        // Listen to the input edge.
        inputEdge.getEdge().setListener(
                inputPacket -> {
                    if (inputPacket.getProcessingRequest().isAborted()) {
                        // No-ops if the request is aborted.
                        return;
                    }
                    mBlockingExecutor.execute(() -> processInputPacket(inputPacket));
                });

        mInput2Packet = new ProcessingInput2Packet();
        mImage2JpegBytes = new Image2JpegBytes();
        mJpegBytes2CroppedBitmap = new JpegBytes2CroppedBitmap();
        mBitmap2JpegBytes = new Bitmap2JpegBytes();
        mJpegBytes2Disk = new JpegBytes2Disk();
        mJpegImage2Result = new JpegImage2Result();
        if (inputEdge.getFormat() == YUV_420_888) {
            mJpegBytes2Image = new JpegBytes2Image();
        }
        // No output. The request callback will be invoked to deliver the final result.
        return null;
    }

    @Override
    public void release() {
    }

    /**
     * Processes an {@link InputPacket} and delivers the result to {@link TakePictureManager}.
     */
    @WorkerThread
    void processInputPacket(@NonNull InputPacket inputPacket) {
        ProcessingRequest request = inputPacket.getProcessingRequest();
        try {
            if (inputPacket.getProcessingRequest().isInMemoryCapture()) {
                ImageProxy result = processInMemoryCapture(inputPacket);
                mainThreadExecutor().execute(() -> request.onFinalResult(result));
            } else {
                ImageCapture.OutputFileResults result = processOnDiskCapture(inputPacket);
                mainThreadExecutor().execute(() -> request.onFinalResult(result));
            }
        } catch (ImageCaptureException e) {
            sendError(request, e);
        } catch (RuntimeException e) {
            // For unexpected exceptions, throw an ERROR_UNKNOWN ImageCaptureException.
            sendError(request, new ImageCaptureException(ERROR_UNKNOWN, "Processing failed.", e));
        }
    }

    @NonNull
    @WorkerThread
    ImageCapture.OutputFileResults processOnDiskCapture(@NonNull InputPacket inputPacket)
            throws ImageCaptureException {
        ProcessingRequest request = inputPacket.getProcessingRequest();
        Packet<ImageProxy> originalImage = mInput2Packet.apply(inputPacket);
        Packet<byte[]> jpegBytes = mImage2JpegBytes.apply(
                Image2JpegBytes.In.of(originalImage, request.getJpegQuality()));
        if (jpegBytes.hasCropping()) {
            Packet<Bitmap> croppedBitmap = mJpegBytes2CroppedBitmap.apply(jpegBytes);
            jpegBytes = mBitmap2JpegBytes.apply(
                    Bitmap2JpegBytes.In.of(croppedBitmap, request.getJpegQuality()));
        }
        return mJpegBytes2Disk.apply(
                JpegBytes2Disk.In.of(jpegBytes, requireNonNull(request.getOutputFileOptions())));
    }

    @NonNull
    @WorkerThread
    ImageProxy processInMemoryCapture(@NonNull InputPacket inputPacket)
            throws ImageCaptureException {
        ProcessingRequest request = inputPacket.getProcessingRequest();
        Packet<ImageProxy> image = mInput2Packet.apply(inputPacket);
        if (image.getFormat() == YUV_420_888) {
            Packet<byte[]> jpegPacket = mImage2JpegBytes.apply(
                    Image2JpegBytes.In.of(image, request.getJpegQuality()));
            image = mJpegBytes2Image.apply(jpegPacket);
        }
        return mJpegImage2Result.apply(image);
    }

    /**
     * Sends {@link ImageCaptureException} to {@link TakePictureManager}.
     */
    private static void sendError(@NonNull ProcessingRequest request,
            @NonNull ImageCaptureException e) {
        mainThreadExecutor().execute(() -> request.onProcessFailure(e));
    }

    /**
     * Input packet which is a combination of camera frame and processing request.
     */
    @AutoValue
    abstract static class InputPacket {

        @NonNull
        abstract ProcessingRequest getProcessingRequest();

        @NonNull
        abstract ImageProxy getImageProxy();

        static InputPacket of(@NonNull ProcessingRequest processingRequest,
                @NonNull ImageProxy imageProxy) {
            return new AutoValue_ProcessingNode_InputPacket(processingRequest, imageProxy);
        }
    }

    /**
     * Input edges of {@link ProcessingNode}.
     */
    @AutoValue
    abstract static class In {

        /**
         * Get the single input edge that contains a {@link InputPacket} flow.
         */
        abstract Edge<InputPacket> getEdge();

        /**
         * Gets the format of the image in {@link InputPacket}.
         */
        abstract int getFormat();

        static In of(int format) {
            return new AutoValue_ProcessingNode_In(new Edge<>(), format);
        }
    }

    @VisibleForTesting
    void injectJpegBytes2CroppedBitmapForTesting(
            @NonNull Operation<Packet<byte[]>, Packet<Bitmap>> operation) {
        mJpegBytes2CroppedBitmap = operation;
    }
}
