/*
 * Copyright (C) 2019 The Android Open Source Project
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

package androidx.camera.core;

import android.app.Application;
import android.os.Handler;
import android.util.Log;

import androidx.annotation.IntDef;
import androidx.annotation.IntRange;
import androidx.annotation.OptIn;
import androidx.annotation.RestrictTo;
import androidx.annotation.RestrictTo.Scope;
import androidx.camera.core.impl.CameraDeviceSurfaceManager;
import androidx.camera.core.impl.CameraFactory;
import androidx.camera.core.impl.Config;
import androidx.camera.core.impl.MutableConfig;
import androidx.camera.core.impl.MutableOptionsBundle;
import androidx.camera.core.impl.OptionsBundle;
import androidx.camera.core.impl.QuirkSettings;
import androidx.camera.core.impl.UseCaseConfigFactory;
import androidx.camera.core.internal.TargetConfig;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.Executor;

/**
 * A configuration for adding implementation and user-specific behavior to CameraX.
 *
 * <p>CameraXConfig provides customizable options for camera provider instances that persist for
 * the lifetime of the provider.
 *
 * <p>An implementation of CameraXConfig can be provided by subclassing the
 * {@link Application} object and implementing {@link CameraXConfig.Provider}. Alternatively,
 * other methods configuration exist such as
 * {@link androidx.camera.lifecycle.ProcessCameraProvider#configureInstance(CameraXConfig)}.
 * {@linkplain androidx.camera.lifecycle.ProcessCameraProvider#getInstance(android.content.Context)
 * Examples} of how this is used can be found in the {@link androidx.camera.lifecycle} package.
 *
 * <p>Applications can create and use {@linkplain androidx.camera.camera2.Camera2Config the
 * implementation} of CameraXConfig provided in {@link androidx.camera.camera2}.
 *
 * @see androidx.camera.lifecycle.ProcessCameraProvider#configureInstance(CameraXConfig)
 * @see CameraXConfig.Builder
 */
@SuppressWarnings("HiddenSuperclass")
public final class CameraXConfig implements TargetConfig<CameraX> {

    /**
     * Unknown CameraX config impl type.
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    public static final int CAMERAX_CONFIG_IMPL_TYPE_UNKNOWN = -1;
    /**
     * camera-camera2 CameraX config impl type.
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    public static final int CAMERAX_CONFIG_IMPL_TYPE_CAMERA_CAMERA2 = 0;
    /**
     * camera-camera2-pipe-integration CameraX config impl type.
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    public static final int CAMERAX_CONFIG_IMPL_TYPE_PIPE = 1;

    /**
     * The different implementation types the CameraXConfig can be configured for.
     */
    @IntDef({CAMERAX_CONFIG_IMPL_TYPE_UNKNOWN, CAMERAX_CONFIG_IMPL_TYPE_CAMERA_CAMERA2,
            CAMERAX_CONFIG_IMPL_TYPE_PIPE})
    @Retention(RetentionPolicy.SOURCE)
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public @interface ImplType {
    }

    /**
     * An interface which can be implemented to provide the configuration for CameraX.
     *
     * <p>When implemented by an {@link Application}, this can provide on-demand initialization
     * of CameraX.
     *
     * <p>{@linkplain
     * androidx.camera.lifecycle.ProcessCameraProvider#getInstance(android.content.Context)
     * Examples} of how this is used can be found in the {@link androidx.camera.lifecycle} package.
     */
    public interface Provider {
        /** Returns the configuration to use for initializing an instance of CameraX. */
        @NonNull CameraXConfig getCameraXConfig();
    }

    // Option Declarations:
    // *********************************************************************************************

    static final Option<CameraFactory.Provider> OPTION_CAMERA_FACTORY_PROVIDER =
            Option.create("camerax.core.appConfig.cameraFactoryProvider",
                    CameraFactory.Provider.class);
    static final Option<CameraDeviceSurfaceManager.Provider>
            OPTION_DEVICE_SURFACE_MANAGER_PROVIDER =
            Option.create(
                    "camerax.core.appConfig.deviceSurfaceManagerProvider",
                    CameraDeviceSurfaceManager.Provider.class);
    static final Option<UseCaseConfigFactory.Provider> OPTION_USECASE_CONFIG_FACTORY_PROVIDER =
            Option.create(
                    "camerax.core.appConfig.useCaseConfigFactoryProvider",
                    UseCaseConfigFactory.Provider.class);
    static final Option<Executor> OPTION_CAMERA_EXECUTOR =
            Option.create(
                    "camerax.core.appConfig.cameraExecutor",
                    Executor.class);
    static final Option<Handler> OPTION_SCHEDULER_HANDLER =
            Option.create(
                    "camerax.core.appConfig.schedulerHandler",
                    Handler.class);
    static final Option<Integer> OPTION_MIN_LOGGING_LEVEL =
            Option.create(
                    "camerax.core.appConfig.minimumLoggingLevel",
                    int.class);
    static final Option<CameraSelector> OPTION_AVAILABLE_CAMERAS_LIMITER =
            Option.create(
                    "camerax.core.appConfig.availableCamerasLimiter",
                    CameraSelector.class);

    static final Option<Long> OPTION_CAMERA_OPEN_RETRY_MAX_TIMEOUT_IN_MILLIS_WHILE_RESUMING =
            Option.create(
                    "camerax.core.appConfig.cameraOpenRetryMaxTimeoutInMillisWhileResuming",
                    long.class);

    @OptIn(markerClass = ExperimentalRetryPolicy.class)
    static final Option<RetryPolicy> OPTION_CAMERA_PROVIDER_INIT_RETRY_POLICY =
            Option.create(
                    "camerax.core.appConfig.cameraProviderInitRetryPolicy",
                    RetryPolicy.class);

    static final long DEFAULT_OPTION_CAMERA_OPEN_RETRY_MAX_TIMEOUT_IN_MILLIS_WHILE_RESUMING = -1L;

    static final Option<QuirkSettings> OPTION_QUIRK_SETTINGS =
            Option.create(
                    "camerax.core.appConfig.quirksSettings",
                    QuirkSettings.class);

    static final Option<Integer> OPTION_CONFIG_IMPL_TYPE =
            Option.create("camerax.core.appConfig.configImplType", int.class);

    // *********************************************************************************************

    private final OptionsBundle mConfig;

    CameraXConfig(OptionsBundle options) {
        mConfig = options;
    }

    /**
     * Returns the {@link CameraFactory} implementation for the application.
     *
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    public CameraFactory.@Nullable Provider getCameraFactoryProvider(
            CameraFactory.@Nullable Provider valueIfMissing) {
        return mConfig.retrieveOption(OPTION_CAMERA_FACTORY_PROVIDER, valueIfMissing);
    }

    /**
     * Returns the {@link CameraDeviceSurfaceManager} implementation for the application.
     *
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    public CameraDeviceSurfaceManager.@Nullable Provider getDeviceSurfaceManagerProvider(
            CameraDeviceSurfaceManager.@Nullable Provider valueIfMissing) {
        return mConfig.retrieveOption(OPTION_DEVICE_SURFACE_MANAGER_PROVIDER, valueIfMissing);
    }

    /**
     * Returns the {@link UseCaseConfigFactory} implementation for the application.
     *
     * <p>This factory should produce all default configurations for the application's use cases.
     *
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    public UseCaseConfigFactory.@Nullable Provider getUseCaseConfigFactoryProvider(
            UseCaseConfigFactory.@Nullable Provider valueIfMissing) {
        return mConfig.retrieveOption(OPTION_USECASE_CONFIG_FACTORY_PROVIDER, valueIfMissing);
    }

    /**
     * Returns the camera executor which CameraX will use to drive the camera stack.
     *
     * @see Builder#setCameraExecutor(Executor)
     */
    public @Nullable Executor getCameraExecutor(@Nullable Executor valueIfMissing) {
        return mConfig.retrieveOption(OPTION_CAMERA_EXECUTOR, valueIfMissing);
    }

    /**
     * Returns the scheduling handler that CameraX will use internally for scheduling future tasks.
     *
     * @see Builder#setSchedulerHandler(Handler)
     */
    public @Nullable Handler getSchedulerHandler(@Nullable Handler valueIfMissing) {
        return mConfig.retrieveOption(OPTION_SCHEDULER_HANDLER, valueIfMissing);
    }

    /**
     * Returns the minimum logging level to be used for CameraX logs.
     *
     * @see Builder#setMinimumLoggingLevel(int)
     */
    public int getMinimumLoggingLevel() {
        return mConfig.retrieveOption(OPTION_MIN_LOGGING_LEVEL, Logger.DEFAULT_MIN_LOG_LEVEL);
    }

    /**
     * Returns the {@link CameraSelector} used to determine the available cameras.
     *
     * @see Builder#setAvailableCamerasLimiter(CameraSelector)
     */
    public @Nullable CameraSelector getAvailableCamerasLimiter(
            @Nullable CameraSelector valueIfMissing) {
        return mConfig.retrieveOption(OPTION_AVAILABLE_CAMERAS_LIMITER, valueIfMissing);
    }

    /**
     * Returns the camera open retry maximum timeout in milliseconds when in active resuming mode.
     *
     * <p>If this value is not set, -1L will be returned by default.
     *
     * @see Builder#setCameraOpenRetryMaxTimeoutInMillisWhileResuming(long)
     */
    public long getCameraOpenRetryMaxTimeoutInMillisWhileResuming() {
        return mConfig.retrieveOption(OPTION_CAMERA_OPEN_RETRY_MAX_TIMEOUT_IN_MILLIS_WHILE_RESUMING,
                DEFAULT_OPTION_CAMERA_OPEN_RETRY_MAX_TIMEOUT_IN_MILLIS_WHILE_RESUMING);
    }

    /**
     * Retrieves the {@link RetryPolicy} for the CameraProvider initialization. This policy
     * determines whether to retry the CameraProvider initialization if it fails.
     *
     * @return The {@link RetryPolicy} to be used for the CameraProvider initialization. If not
     * explicitly set, it defaults to {@link RetryPolicy#DEFAULT}.
     *
     * @see Builder#setCameraProviderInitRetryPolicy(RetryPolicy)
     */
    @ExperimentalRetryPolicy
    public @NonNull RetryPolicy getCameraProviderInitRetryPolicy() {
        return Objects.requireNonNull(
                mConfig.retrieveOption(OPTION_CAMERA_PROVIDER_INIT_RETRY_POLICY,
                        RetryPolicy.DEFAULT));
    }

    /**
     * Returns the quirk settings.
     *
     * <p>If this value is not set, a default quirk settings will be returned.
     *
     * @return the quirk settings.
     *
     * @see Builder#setQuirkSettings(QuirkSettings)
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    public @Nullable QuirkSettings getQuirkSettings() {
        return mConfig.retrieveOption(OPTION_QUIRK_SETTINGS, null);
    }

    /**
     * Returns the config impl type.
     *
     * @return the config impl type.
     *
     * @see Builder#setConfigImplType(int)
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    public @ImplType int getConfigImplType() {
        return mConfig.retrieveOption(OPTION_CONFIG_IMPL_TYPE, CAMERAX_CONFIG_IMPL_TYPE_UNKNOWN);
    }

    @RestrictTo(Scope.LIBRARY_GROUP)
    @Override
    public @NonNull Config getConfig() {
        return mConfig;
    }

    /** A builder for generating {@link CameraXConfig} objects. */
    @SuppressWarnings({"ObjectToString", "HiddenSuperclass"})
    public static final class Builder
            implements TargetConfig.Builder<CameraX, CameraXConfig.Builder> {

        private final MutableOptionsBundle mMutableConfig;

        /**
         * Creates a new Builder object.
         *
         */
        @RestrictTo(Scope.LIBRARY_GROUP)
        public Builder() {
            this(MutableOptionsBundle.create());
        }

        private Builder(MutableOptionsBundle mutableConfig) {
            mMutableConfig = mutableConfig;

            Class<?> oldConfigClass =
                    mutableConfig.retrieveOption(TargetConfig.OPTION_TARGET_CLASS, null);
            if (oldConfigClass != null && !oldConfigClass.equals(CameraX.class)) {
                throw new IllegalArgumentException(
                        "Invalid target class configuration for "
                                + CameraXConfig.Builder.this
                                + ": "
                                + oldConfigClass);
            }

            setTargetClass(CameraX.class);
        }

        /**
         * Generates a Builder from another {@link CameraXConfig} object
         *
         * @param configuration An immutable configuration to pre-populate this builder.
         * @return The new Builder.
         */
        public static @NonNull Builder fromConfig(@NonNull CameraXConfig configuration) {
            return new Builder(MutableOptionsBundle.from(configuration));
        }

        /**
         * Sets the {@link CameraFactory} implementation for the application.
         *
         */
        @RestrictTo(Scope.LIBRARY_GROUP)
        public @NonNull Builder setCameraFactoryProvider(
                CameraFactory.@NonNull Provider cameraFactory) {
            getMutableConfig().insertOption(OPTION_CAMERA_FACTORY_PROVIDER, cameraFactory);
            return this;
        }

        /**
         * Sets the {@link CameraDeviceSurfaceManager} implementation for the application.
         *
         */
        @RestrictTo(Scope.LIBRARY_GROUP)
        public @NonNull Builder setDeviceSurfaceManagerProvider(
                CameraDeviceSurfaceManager.@NonNull Provider surfaceManagerProvider) {
            getMutableConfig().insertOption(OPTION_DEVICE_SURFACE_MANAGER_PROVIDER,
                    surfaceManagerProvider);
            return this;
        }

        /**
         * Sets the {@link UseCaseConfigFactory} implementation for the application.
         *
         * <p>This factory should produce all default configurations for the application's use
         * cases.
         *
         */
        @RestrictTo(Scope.LIBRARY_GROUP)
        public @NonNull Builder setUseCaseConfigFactoryProvider(
                UseCaseConfigFactory.@NonNull Provider configFactoryProvider) {
            getMutableConfig().insertOption(OPTION_USECASE_CONFIG_FACTORY_PROVIDER,
                    configFactoryProvider);
            return this;
        }

        /**
         * Sets an executor which CameraX will use to drive the camera stack.
         *
         * <p>This option can be used to override the default internal executor created by
         * CameraX, and will be used by the implementation to drive all cameras.
         *
         * <p>It is not necessary to set an executor for normal use, and should only be used in
         * applications with very specific threading requirements. If not set, CameraX will
         * create and use an optimized default internal executor.
         */
        public @NonNull Builder setCameraExecutor(@NonNull Executor executor) {
            getMutableConfig().insertOption(OPTION_CAMERA_EXECUTOR, executor);
            return this;
        }

        /**
         * Sets a handler that CameraX will use internally for scheduling future tasks.
         *
         * <p>This scheduler may also be used for legacy APIs which require a {@link Handler}. Tasks
         * that are scheduled with this handler will always be executed by the camera executor. No
         * business logic will be executed directly by this handler.
         *
         * <p>It is not necessary to set a scheduler handler for normal use, and should only be
         * used in applications with very specific threading requirements. If not set, CameraX
         * will create and use an optimized default internal handler.
         *
         * @see #setCameraExecutor(Executor)
         */
        public @NonNull Builder setSchedulerHandler(@NonNull Handler handler) {
            getMutableConfig().insertOption(OPTION_SCHEDULER_HANDLER, handler);
            return this;
        }

        /**
         * Sets the minimum logging level to be used for CameraX logs.
         * <p>
         * The logging level should be one of the following: {@link Log#DEBUG}, {@link Log#INFO},
         * {@link Log#WARN} or {@link Log#ERROR}.
         * <p>
         * When not specified, the default minimum logging level used inside CameraX is
         * {@link Log#DEBUG}.
         * <p>
         * For apps that want to reduce the logs produced by CameraX, set it to {@link Log#ERROR}
         * to avoid all logs except for error.
         *
         * @param logLevel The minimum logging level, which should be {@link Log#DEBUG},
         *                 {@link Log#INFO}, {@link Log#WARN} or {@link Log#ERROR}.
         * @return This {@link Builder} instance.
         */
        public @NonNull Builder setMinimumLoggingLevel(
                @IntRange(from = Log.DEBUG, to = Log.ERROR) int logLevel) {
            getMutableConfig().insertOption(OPTION_MIN_LOGGING_LEVEL, logLevel);
            return this;
        }

        /**
         * Sets a {@link CameraSelector} to determine the available cameras, thus defining which
         * cameras can be used in the application.
         *
         * <p>Only cameras selected by this CameraSelector can be used in the application. If the
         * application binds use cases with a CameraSelector that selects an unavailable camera,
         * an {@link IllegalArgumentException} will be thrown.
         *
         * <p>This configuration can help CameraX optimize the latency of CameraX initialization.
         * The tasks CameraX initialization performs include enumerating cameras, querying
         * camera characteristics and retrieving properties in preparation for resolution
         * determination. On some low end devices, these tasks could take a significant amount of
         * time. Using this method can avoid the initialization of unnecessary cameras and speed
         * up the time for camera start-up. For example, if the application uses only back facing
         * cameras, it can set this configuration with {@link CameraSelector#DEFAULT_BACK_CAMERA}
         * and then CameraX will avoid initializing front facing cameras to reduce the latency.
         */
        public @NonNull Builder setAvailableCamerasLimiter(
                @NonNull CameraSelector availableCameraSelector) {
            getMutableConfig().insertOption(OPTION_AVAILABLE_CAMERAS_LIMITER,
                    availableCameraSelector);
            return this;
        }

        /**
         * Sets the camera open retry maximum timeout in milliseconds. This is only needed when
         * users don't want to retry camera opening for a long time.
         *
         * <p>When {@link androidx.lifecycle.LifecycleOwner} is in ON_RESUME state, CameraX will
         * actively retry opening the camera periodically to resume, until there is
         * non-recoverable errors happening and then move to pending open state waiting for the
         * next camera available after timeout.
         *
         * <p>When in active resuming mode, it will periodically retry opening the
         * camera regardless of the camera availability.
         * Elapsed time <= 2 minutes -> retry once per 1 second.
         * Elapsed time 2 to 5 minutes -> retry once per 2 seconds.
         * Elapsed time > 5 minutes -> retry once per 4 seconds.
         * Retry will stop after 30 minutes.
         *
         * <p>When not in active resuming mode, the camera will be attempted to be opened every
         * 700ms for 10 seconds. This value cannot currently be changed.
         *
         * @param maxTimeoutInMillis The max timeout in milliseconds.
         * @return this builder.
         */
        public @NonNull Builder setCameraOpenRetryMaxTimeoutInMillisWhileResuming(
                long maxTimeoutInMillis) {
            getMutableConfig().insertOption(
                    OPTION_CAMERA_OPEN_RETRY_MAX_TIMEOUT_IN_MILLIS_WHILE_RESUMING,
                    maxTimeoutInMillis);
            return this;
        }

        /**
         * Sets the {@link RetryPolicy} for the CameraProvider initialization. This policy
         * determines whether to retry the CameraProvider initialization if it fails.
         *
         * <p>If not set, a default retry policy {@link RetryPolicy#DEFAULT} will be applied.
         *
         * @param retryPolicy The {@link RetryPolicy} to use for retrying the CameraProvider
         *                    initialization.
         * @return this builder.
         */
        @ExperimentalRetryPolicy
        public @NonNull Builder setCameraProviderInitRetryPolicy(@NonNull RetryPolicy retryPolicy) {
            getMutableConfig().insertOption(
                    OPTION_CAMERA_PROVIDER_INIT_RETRY_POLICY,
                    retryPolicy);
            return this;
        }

        /**
         * Sets the quirk settings.
         *
         * @param quirkSettings the quirk settings.
         * @return this builder.
         */
        @RestrictTo(Scope.LIBRARY_GROUP)
        public @NonNull Builder setQuirkSettings(@NonNull QuirkSettings quirkSettings) {
            getMutableConfig().insertOption(OPTION_QUIRK_SETTINGS, quirkSettings);
            return this;
        }

        private @NonNull MutableConfig getMutableConfig() {
            return mMutableConfig;
        }

        /**
         * Builds an immutable {@link CameraXConfig} from the current state.
         *
         * @return A {@link CameraXConfig} populated with the current state.
         */
        public @NonNull CameraXConfig build() {
            return new CameraXConfig(OptionsBundle.from(mMutableConfig));
        }

        // Implementations of TargetConfig.Builder default methods

        @RestrictTo(Scope.LIBRARY_GROUP)
        @Override
        public @NonNull Builder setTargetClass(@NonNull Class<CameraX> targetClass) {
            getMutableConfig().insertOption(OPTION_TARGET_CLASS, targetClass);

            // If no name is set yet, then generate a unique name
            if (null == getMutableConfig().retrieveOption(OPTION_TARGET_NAME, null)) {
                String targetName = targetClass.getCanonicalName() + "-" + UUID.randomUUID();
                setTargetName(targetName);
            }

            return this;
        }

        @RestrictTo(Scope.LIBRARY_GROUP)
        @Override
        public @NonNull Builder setTargetName(@NonNull String targetName) {
            getMutableConfig().insertOption(OPTION_TARGET_NAME, targetName);
            return this;
        }

        /**
         * Sets the config impl type.
         *
         * <p>The available impl types are {@link #CAMERAX_CONFIG_IMPL_TYPE_CAMERA_CAMERA2},
         * {@link #CAMERAX_CONFIG_IMPL_TYPE_PIPE} and {@link #CAMERAX_CONFIG_IMPL_TYPE_UNKNOWN}.
         */
        @RestrictTo(Scope.LIBRARY_GROUP)
        public @NonNull Builder setConfigImplType(@ImplType int configImplType) {
            getMutableConfig().insertOption(OPTION_CONFIG_IMPL_TYPE, configImplType);
            return this;
        }
    }
}
