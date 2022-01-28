/*
 * Copyright 2021 HIMSA II K/S - www.himsa.com.
 * Represented by EHIMA - www.ehima.com
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

package android.bluetooth;

import static android.bluetooth.BluetoothUtils.getSyncTimeout;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.annotation.RequiresPermission;
import android.annotation.SdkConstant;
import android.bluetooth.annotations.RequiresBluetoothConnectPermission;
import android.content.AttributionSource;
import android.content.Context;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.CloseGuard;
import android.util.Log;

import com.android.modules.utils.SynchronousResultReceiver;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeoutException;


/**
 * This class provides a public APIs to control the Bluetooth Hearing Access Profile client service.
 *
 * <p>BluetoothHapClient is a proxy object for controlling the Bluetooth HAP
 * Service client via IPC. Use {@link BluetoothAdapter#getProfileProxy} to get the
 * BluetoothHapClient proxy object.
 */
public final class BluetoothHapClient implements BluetoothProfile, AutoCloseable {
    private static final String TAG = "BluetoothHapClient";
    private static final boolean DBG = false;
    private static final boolean VDBG = false;

    private CloseGuard mCloseGuard;

    /**
     * Intent used to broadcast the change in connection state of the Hearing Access Profile Client
     * service. Please note that in the binaural case, there will be two different LE devices for
     * the left and right side and each device will have their own connection state changes.
     *
     * <p>This intent will have 3 extras:
     * <ul>
     * <li> {@link #EXTRA_STATE} - The current state of the profile. </li>
     * <li> {@link #EXTRA_PREVIOUS_STATE}- The previous state of the profile.</li>
     * <li> {@link BluetoothDevice#EXTRA_DEVICE} - The remote device. </li>
     * </ul>
     *
     * <p>{@link #EXTRA_STATE} or {@link #EXTRA_PREVIOUS_STATE} can be any of
     * {@link #STATE_DISCONNECTED}, {@link #STATE_CONNECTING},
     * {@link #STATE_CONNECTED}, {@link #STATE_DISCONNECTING}.
     */
    @RequiresBluetoothConnectPermission
    @RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT)
    @SdkConstant(SdkConstant.SdkConstantType.BROADCAST_INTENT_ACTION)
    public static final String ACTION_HAP_CONNECTION_STATE_CHANGED =
            "android.bluetooth.action.HAP_CONNECTION_STATE_CHANGED";

    /**
     * Intent used to broadcast the device availability change and the availability of its
     * presets. Please note that in the binaural case, there will be two different LE devices for
     * the left and right side and each device will have their own availability event.
     *
     * <p>This intent will have 2 extras:
     * <ul>
     * <li> {@link BluetoothDevice#EXTRA_DEVICE} - The remote device. </li>
     * <li> {@link #EXTRA_HAP_FEATURES} - Supported features map. </li>
     * </ul>
     *
     * @hide
     */
    @RequiresPermission(allOf = {
            android.Manifest.permission.BLUETOOTH_CONNECT,
            android.Manifest.permission.BLUETOOTH_PRIVILEGED,
    })
    @SdkConstant(SdkConstant.SdkConstantType.BROADCAST_INTENT_ACTION)
    public static final String ACTION_HAP_DEVICE_AVAILABLE =
            "android.bluetooth.action.HAP_DEVICE_AVAILABLE";

    /**
     * Intent used to broadcast HA device's feature set.
     *
     * <p>This intent will have 2 extras:
     * <ul>
     * <li> {@link BluetoothDevice#EXTRA_DEVICE} - The remote device. </li>
     * <li> {@link #EXTRA_HAP_FEATURES}- The feature set integer with these possible bit numbers
     * set: {@link #FEATURE_BIT_NUM_TYPE_MONAURAL}, {@link #FEATURE_BIT_NUM_TYPE_BANDED},
     * {@link #FEATURE_BIT_NUM_SYNCHRONIZATED_PRESETS},
     * {@link #FEATURE_BIT_NUM_INDEPENDENT_PRESETS}, {@link #FEATURE_BIT_NUM_DYNAMIC_PRESETS},
     * {@link #FEATURE_BIT_NUM_WRITABLE_PRESETS}.</li>
     * </ul>
     *
     * @hide
     */
    @RequiresPermission(allOf = {
            android.Manifest.permission.BLUETOOTH_CONNECT,
            android.Manifest.permission.BLUETOOTH_PRIVILEGED,
    })
    @SdkConstant(SdkConstant.SdkConstantType.BROADCAST_INTENT_ACTION)
    public static final String ACTION_HAP_ON_DEVICE_FEATURES =
            "android.bluetooth.action.HAP_ON_DEVICE_FEATURES";

    /**
     * Intent used to broadcast the change of a HA device's active preset.
     *
     * <p>This intent will have 2 extras:
     * <ul>
     * <li> {@link BluetoothDevice#EXTRA_DEVICE} - The remote device. </li>
     * <li> {@link #EXTRA_HAP_PRESET_INDEX}- The currently active preset.</li>
     * </ul>
     *
     * @hide
     */
    @RequiresPermission(allOf = {
            android.Manifest.permission.BLUETOOTH_CONNECT,
            android.Manifest.permission.BLUETOOTH_PRIVILEGED,
    })
    @SdkConstant(SdkConstant.SdkConstantType.BROADCAST_INTENT_ACTION)
    public static final String ACTION_HAP_ON_ACTIVE_PRESET =
            "android.bluetooth.action.HAP_ON_ACTIVE_PRESET";

    /**
     * Intent used to broadcast the result of a failed preset change attempt.
     *
     * <p>This intent will have 2 extras:
     * <ul>
     * <li> {@link BluetoothDevice#EXTRA_DEVICE} - The remote device. </li>
     * <li> {@link #EXTRA_HAP_STATUS_CODE}- Failure reason.</li>
     * </ul>
     *
     * <p>{@link #EXTRA_HAP_STATUS_CODE} can be any of {@link #STATUS_INVALID_PRESET_INDEX},
     * {@link #STATUS_OPERATION_NOT_POSSIBLE},{@link #STATUS_OPERATION_NOT_SUPPORTED}.
     *
     * @hide
     */
    @RequiresPermission(allOf = {
            android.Manifest.permission.BLUETOOTH_CONNECT,
            android.Manifest.permission.BLUETOOTH_PRIVILEGED,
    })
    @SdkConstant(SdkConstant.SdkConstantType.BROADCAST_INTENT_ACTION)
    public static final String ACTION_HAP_ON_ACTIVE_PRESET_SELECT_ERROR =
            "android.bluetooth.action.HAP_ON_ACTIVE_PRESET_SELECT_ERROR";

    /**
     * Intent used to broadcast preset name change.
     *
     * <p>This intent will have 4 extras:
     * <ul>
     * <li> {@link BluetoothDevice#EXTRA_DEVICE} - The remote device. </li>
     * <li> {@link #EXTRA_HAP_PRESET_INFO}- List of preset informations </li>
     * <li> {@link #EXTRA_HAP_PRESET_INFO_REASON}- Why this preset info notification was sent </li>
     * notifications or the user should expect more to come. </li>
     * </ul>
     *
     * @hide
     */
    @RequiresPermission(allOf = {
            android.Manifest.permission.BLUETOOTH_CONNECT,
            android.Manifest.permission.BLUETOOTH_PRIVILEGED,
    })
    @SdkConstant(SdkConstant.SdkConstantType.BROADCAST_INTENT_ACTION)
    public static final String ACTION_HAP_ON_PRESET_INFO =
            "android.bluetooth.action.HAP_ON_PRESET_INFO";

    /**
     * Intent used to broadcast result of a failed rename attempt.
     *
     * <p>This intent will have 3 extras:
     * <ul>
     * <li> {@link BluetoothDevice#EXTRA_DEVICE} - The remote device. </li>
     * <li> {@link #EXTRA_HAP_PRESET_INDEX}- The currently active preset.</li>
     * <li> {@link #EXTRA_HAP_STATUS_CODE}- Failure reason code.</li>
     * </ul>
     *
     * <p>{@link #EXTRA_HAP_STATUS_CODE} can be any of {@link #STATUS_SET_NAME_NOT_ALLOWED},
     * {@link #STATUS_INVALID_PRESET_INDEX}, {@link #STATUS_INVALID_PRESET_NAME_LENGTH}.
     *
     * @hide
     */
    @RequiresPermission(allOf = {
            android.Manifest.permission.BLUETOOTH_CONNECT,
            android.Manifest.permission.BLUETOOTH_PRIVILEGED,
    })
    @SdkConstant(SdkConstant.SdkConstantType.BROADCAST_INTENT_ACTION)
    public static final String ACTION_HAP_ON_PRESET_NAME_SET_ERROR =
            "android.bluetooth.action.HAP_ON_PRESET_NAME_SET_ERROR";

    /**
     * Intent used to broadcast the result of a failed name get attempt.
     *
     * <p>This intent will have 3 extras:
     * <ul>
     * <li> {@link BluetoothDevice#EXTRA_DEVICE} - The remote device. </li>
     * <li> {@link #EXTRA_HAP_PRESET_INDEX}- The currently active preset.</li>
     * <li> {@link #EXTRA_HAP_STATUS_CODE}- Failure reason code.</li>
     * </ul>
     *
     * <p>{@link #EXTRA_HAP_STATUS_CODE} can be any of {@link #STATUS_INVALID_PRESET_INDEX},
     * {@link #STATUS_OPERATION_NOT_POSSIBLE}.
     *
     * @hide
     */
    @RequiresPermission(allOf = {
            android.Manifest.permission.BLUETOOTH_CONNECT,
            android.Manifest.permission.BLUETOOTH_PRIVILEGED,
    })
    @SdkConstant(SdkConstant.SdkConstantType.BROADCAST_INTENT_ACTION)
    public static final String ACTION_HAP_ON_PRESET_INFO_GET_ERROR =
            "android.bluetooth.action.HAP_ON_PRESET_INFO_GET_ERROR";

    /**
     * Contains a list of all available presets
     * @hide
     */
    public static final String EXTRA_HAP_FEATURES = "android.bluetooth.extra.HAP_FEATURES";

    /**
     * Contains a preset identifier
     * @hide
     */
    public static final String EXTRA_HAP_PRESET_INDEX = "android.bluetooth.extra.HAP_PRESET_INDEX";

    /**
     * Used to report failure reasons.
     * @hide
     */
    public static final String EXTRA_HAP_STATUS_CODE = "android.bluetooth.extra.HAP_STATUS_CODE";

    /**
     * Used by group events.
     * @hide
     */
    public static final String EXTRA_HAP_GROUP_ID = "android.bluetooth.extra.HAP_GROUP_ID";

    /**
     * Preset Info reason.
     * Possible values:
     *  {@link #PRESET_INFO_REASON_ALL_PRESET_INFO} or
     *  {@link #PRESET_INFO_REASON_PRESET_INFO_UPDATE} or
     *  {@link #PRESET_INFO_REASON_PRESET_DELETED} or
     *  {@link #PRESET_INFO_REASON_PRESET_AVAILABILITY_CHANGED} or
     *  {@link #PRESET_INFO_REASON_PRESET_INFO_REQUEST_RESPONSE}
     * @hide
     */
    public static final String EXTRA_HAP_PRESET_INFO_REASON =
            "android.bluetooth.extra.HAP_PRESET_INFO_REASON";

    /**
     * Preset Info.
     * @hide
     */
    public static final String EXTRA_HAP_PRESET_INFO = "android.bluetooth.extra.HAP_PRESET_INFO";

    /**
     * Preset name change failure due to preset being read-only.
     * @hide
     */
    public static final int STATUS_SET_NAME_NOT_ALLOWED =
            IBluetoothHapClient.STATUS_SET_NAME_NOT_ALLOWED;

    /**
     * Means that the requested operation is not supported by the HA device.
     *
     * <p> It could mean that the requested name change is not supported on
     * a given preset or the device does not support presets at all.
     * @hide
     */
    public static final int STATUS_OPERATION_NOT_SUPPORTED =
            IBluetoothHapClient.STATUS_OPERATION_NOT_SUPPORTED;

    /**
     * Usually means a temporary denial of certain operation. Peer device may report this
     * status due to various implementation specific reasons. It's different than
     * the {@link #STATUS_OPERATION_NOT_SUPPORTED} which represents more of a
     * permanent inability to perform some of the operations.
     * @hide
     */
    public static final int STATUS_OPERATION_NOT_POSSIBLE =
            IBluetoothHapClient.STATUS_OPERATION_NOT_POSSIBLE;

    /**
     * Used when preset name change failed due to the passed name parameter being to long.
     * @hide
     */
    public static final int STATUS_INVALID_PRESET_NAME_LENGTH =
            IBluetoothHapClient.STATUS_INVALID_PRESET_NAME_LENGTH;

    /**
     * Group operations are not supported.
     * @hide
     */
    public static final int STATUS_GROUP_OPERATION_NOT_SUPPORTED =
            IBluetoothHapClient.STATUS_GROUP_OPERATION_NOT_SUPPORTED;

    /**
     * Procedure is already in progress.
     * @hide
     */
    public static final int STATUS_PROCEDURE_ALREADY_IN_PROGRESS =
            IBluetoothHapClient.STATUS_PROCEDURE_ALREADY_IN_PROGRESS;

    /**
     * Invalid preset index input parameter used in one of the API calls.
     * @hide
     */
    public static final int STATUS_INVALID_PRESET_INDEX =
            IBluetoothHapClient.STATUS_INVALID_PRESET_INDEX;

    /**
     * Represets an invalid index value. This is usually value returned in a currently
     * active preset request for a device which is not connected. This value shouldn't be used
     * in the API calls.
     * @hide
     */
    public static final int PRESET_INDEX_UNAVAILABLE = IBluetoothHapClient.PRESET_INDEX_UNAVAILABLE;

    /**
     * Feature bit.
     * @hide
     */
    public static final int FEATURE_BIT_NUM_TYPE_MONAURAL =
            IBluetoothHapClient.FEATURE_BIT_NUM_TYPE_MONAURAL;

    /**
     * Feature bit.
     * @hide
     */
    public static final int FEATURE_BIT_NUM_TYPE_BANDED =
            IBluetoothHapClient.FEATURE_BIT_NUM_TYPE_BANDED;

    /**
     * Feature bit.
     * @hide
     */
    public static final int FEATURE_BIT_NUM_SYNCHRONIZATED_PRESETS =
            IBluetoothHapClient.FEATURE_BIT_NUM_SYNCHRONIZATED_PRESETS;

    /**
     * Feature bit.
     * @hide
     */
    public static final int FEATURE_BIT_NUM_INDEPENDENT_PRESETS =
            IBluetoothHapClient.FEATURE_BIT_NUM_INDEPENDENT_PRESETS;

    /**
     * Feature bit.
     * @hide
     */
    public static final int FEATURE_BIT_NUM_DYNAMIC_PRESETS =
            IBluetoothHapClient.FEATURE_BIT_NUM_DYNAMIC_PRESETS;

    /**
     * Feature bit.
     * @hide
     */
    public static final int FEATURE_BIT_NUM_WRITABLE_PRESETS =
            IBluetoothHapClient.FEATURE_BIT_NUM_WRITABLE_PRESETS;

    /**
     * Preset Info notification reason.
     * @hide
     */
    public static final int PRESET_INFO_REASON_ALL_PRESET_INFO =
            IBluetoothHapClient.PRESET_INFO_REASON_ALL_PRESET_INFO;

    /**
     * Preset Info notification reason.
     * @hide
     */
    public static final int PRESET_INFO_REASON_PRESET_INFO_UPDATE =
            IBluetoothHapClient.PRESET_INFO_REASON_PRESET_INFO_UPDATE;

    /**
     * Preset Info notification reason.
     * @hide
     */
    public static final int PRESET_INFO_REASON_PRESET_DELETED =
            IBluetoothHapClient.PRESET_INFO_REASON_PRESET_DELETED;

    /**
     * Preset Info notification reason.
     * @hide
     */
    public static final int PRESET_INFO_REASON_PRESET_AVAILABILITY_CHANGED =
            IBluetoothHapClient.PRESET_INFO_REASON_PRESET_AVAILABILITY_CHANGED;

    /**
     * Preset Info notification reason.
     * @hide
     */
    public static final int PRESET_INFO_REASON_PRESET_INFO_REQUEST_RESPONSE =
            IBluetoothHapClient.PRESET_INFO_REASON_PRESET_INFO_REQUEST_RESPONSE;

    /**
     * Represents invalid group identifier. It's returned when user requests a group identifier
     * for a device which is not part of any group. This value shouldn't be used in the API calls.
     * @hide
     */
    public static final int HAP_GROUP_UNAVAILABLE = IBluetoothHapClient.GROUP_ID_UNAVAILABLE;

    private final BluetoothAdapter mAdapter;
    private final AttributionSource mAttributionSource;
    private final BluetoothProfileConnector<IBluetoothHapClient> mProfileConnector =
            new BluetoothProfileConnector(this, BluetoothProfile.HAP_CLIENT, "BluetoothHapClient",
                    IBluetoothHapClient.class.getName()) {
                @Override
                public IBluetoothHapClient getServiceInterface(IBinder service) {
                    return IBluetoothHapClient.Stub.asInterface(service);
                }
            };

    /**
     * Create a BluetoothHapClient proxy object for interacting with the local
     * Bluetooth Hearing Access Profile (HAP) client.
     */
    /*package*/ BluetoothHapClient(Context context, ServiceListener listener) {
        mAdapter = BluetoothAdapter.getDefaultAdapter();
        mAttributionSource = mAdapter.getAttributionSource();
        mProfileConnector.connect(context, listener);
        mCloseGuard = new CloseGuard();
        mCloseGuard.open("close");
    }

    /**
     * @hide
     */
    protected void finalize() {
        if (mCloseGuard != null) {
            mCloseGuard.warnIfOpen();
        }
        close();
    }

    /**
     * @hide
     */
    public void close() {
        mProfileConnector.disconnect();
    }

    private IBluetoothHapClient getService() {
        return mProfileConnector.getService();
    }

    /**
     * Set connection policy of the profile
     *
     * <p> The device should already be paired.
     * Connection policy can be one of {@link #CONNECTION_POLICY_ALLOWED},
     * {@link #CONNECTION_POLICY_FORBIDDEN}, {@link #CONNECTION_POLICY_UNKNOWN}
     *
     * @param device Paired bluetooth device
     * @param connectionPolicy is the connection policy to set to for this profile
     * @return true if connectionPolicy is set, false on error
     * @hide
     */
    @RequiresBluetoothConnectPermission
    @RequiresPermission(allOf = {
            android.Manifest.permission.BLUETOOTH_CONNECT,
            android.Manifest.permission.BLUETOOTH_PRIVILEGED,
    })
    public boolean setConnectionPolicy(@NonNull BluetoothDevice device,
            @ConnectionPolicy int connectionPolicy) {
        if (DBG) log("setConnectionPolicy(" + device + ", " + connectionPolicy + ")");
        final IBluetoothHapClient service = getService();
        final boolean defaultValue = false;
        if (service == null) {
            Log.w(TAG, "Proxy not attached to service");
            if (DBG) log(Log.getStackTraceString(new Throwable()));
        } else if (mAdapter.isEnabled() && isValidDevice(device)
                    && (connectionPolicy == BluetoothProfile.CONNECTION_POLICY_FORBIDDEN
                        || connectionPolicy == BluetoothProfile.CONNECTION_POLICY_ALLOWED)) {
            try {
                final SynchronousResultReceiver<Boolean> recv = new SynchronousResultReceiver();
                service.setConnectionPolicy(device, connectionPolicy, mAttributionSource, recv);
                return recv.awaitResultNoInterrupt(getSyncTimeout()).getValue(defaultValue);
            } catch (RemoteException | TimeoutException e) {
                Log.e(TAG, e.toString() + "\n" + Log.getStackTraceString(new Throwable()));
            }
        }
        return defaultValue;
    }

    /**
     * Get the connection policy of the profile.
     *
     * <p> The connection policy can be any of:
     * {@link #CONNECTION_POLICY_ALLOWED}, {@link #CONNECTION_POLICY_FORBIDDEN},
     * {@link #CONNECTION_POLICY_UNKNOWN}
     *
     * @param device Bluetooth device
     * @return connection policy of the device
     * @hide
     */
    @RequiresBluetoothConnectPermission
    @RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT)
    public @ConnectionPolicy int getConnectionPolicy(@Nullable BluetoothDevice device) {
        if (VDBG) log("getConnectionPolicy(" + device + ")");
        final IBluetoothHapClient service = getService();
        final int defaultValue = BluetoothProfile.CONNECTION_POLICY_FORBIDDEN;
        if (service == null) {
            Log.w(TAG, "Proxy not attached to service");
            if (DBG) log(Log.getStackTraceString(new Throwable()));
        } else if (mAdapter.isEnabled() && isValidDevice(device)) {
            try {
                final SynchronousResultReceiver<Integer> recv = new SynchronousResultReceiver();
                service.getConnectionPolicy(device, mAttributionSource, recv);
                return recv.awaitResultNoInterrupt(getSyncTimeout()).getValue(defaultValue);
            } catch (RemoteException | TimeoutException e) {
                Log.e(TAG, e.toString() + "\n" + Log.getStackTraceString(new Throwable()));
            }
        }
        return defaultValue;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @RequiresBluetoothConnectPermission
    @RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT)
    public @NonNull List<BluetoothDevice> getConnectedDevices() {
        if (VDBG) Log.d(TAG, "getConnectedDevices()");
        final IBluetoothHapClient service = getService();
        final List defaultValue = new ArrayList<BluetoothDevice>();
        if (service == null) {
            Log.w(TAG, "Proxy not attached to service");
            if (DBG) log(Log.getStackTraceString(new Throwable()));
        } else if (isEnabled()) {
            try {
                final SynchronousResultReceiver<List> recv = new SynchronousResultReceiver();
                service.getConnectedDevices(mAttributionSource, recv);
                return recv.awaitResultNoInterrupt(getSyncTimeout()).getValue(defaultValue);
            } catch (RemoteException | TimeoutException e) {
                Log.e(TAG, e.toString() + "\n" + Log.getStackTraceString(new Throwable()));
            }
        }
        return defaultValue;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @RequiresBluetoothConnectPermission
    @RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT)
    public @NonNull List<BluetoothDevice> getDevicesMatchingConnectionStates(
            @NonNull int[] states) {
        if (VDBG) Log.d(TAG, "getDevicesMatchingConnectionStates()");
        final IBluetoothHapClient service = getService();
        final List defaultValue = new ArrayList<BluetoothDevice>();
        if (service == null) {
            Log.w(TAG, "Proxy not attached to service");
            if (DBG) log(Log.getStackTraceString(new Throwable()));
        } else if (isEnabled()) {
            try {
                final SynchronousResultReceiver<List> recv = new SynchronousResultReceiver();
                service.getDevicesMatchingConnectionStates(states, mAttributionSource, recv);
                return recv.awaitResultNoInterrupt(getSyncTimeout()).getValue(defaultValue);
            } catch (RemoteException | TimeoutException e) {
                Log.e(TAG, e.toString() + "\n" + Log.getStackTraceString(new Throwable()));
            }
        }
        return defaultValue;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @RequiresBluetoothConnectPermission
    @RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT)
    public @BluetoothProfile.BtProfileState int getConnectionState(
            @NonNull BluetoothDevice device) {
        if (VDBG) Log.d(TAG, "getConnectionState(" + device + ")");
        final IBluetoothHapClient service = getService();
        final int defaultValue = BluetoothProfile.STATE_DISCONNECTED;
        if (service == null) {
            Log.w(TAG, "Proxy not attached to service");
            if (DBG) log(Log.getStackTraceString(new Throwable()));
        } else if (isEnabled() && isValidDevice(device)) {
            try {
                final SynchronousResultReceiver<Integer> recv = new SynchronousResultReceiver();
                service.getConnectionState(device, mAttributionSource, recv);
                return recv.awaitResultNoInterrupt(getSyncTimeout()).getValue(defaultValue);
            } catch (RemoteException | TimeoutException e) {
                Log.e(TAG, e.toString() + "\n" + Log.getStackTraceString(new Throwable()));
            }
        }
        return defaultValue;
    }

    /**
     * Gets the group identifier, which can be used in the group related part of
     * the API.
     *
     * <p>Users are expected to get group identifier for each of the connected
     * device to discover the device grouping. This allows them to make an informed
     * decision which devices can be controlled by single group API call and which
     * require individual device calls.
     *
     * <p>Note that some binaural HA devices may not support group operations,
     * therefore are not considered a valid HAP group. In such case the
     * {@link #HAP_GROUP_UNAVAILABLE} is returned even when such
     * device is a valid Le Audio Coordinated Set member.
     *
     * @param device
     * @return valid group identifier or {@link #HAP_GROUP_UNAVAILABLE}
     * @hide
     */
    @RequiresBluetoothConnectPermission
    @RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT)
    public int getHapGroup(@NonNull BluetoothDevice device) {
        final IBluetoothHapClient service = getService();
        final int defaultValue = HAP_GROUP_UNAVAILABLE;
        if (service == null) {
            Log.w(TAG, "Proxy not attached to service");
            if (DBG) log(Log.getStackTraceString(new Throwable()));
        } else if (isEnabled() && isValidDevice(device)) {
            try {
                final SynchronousResultReceiver<Integer> recv = new SynchronousResultReceiver();
                service.getHapGroup(device, mAttributionSource, recv);
                return recv.awaitResultNoInterrupt(getSyncTimeout()).getValue(defaultValue);
            } catch (RemoteException | TimeoutException e) {
                Log.e(TAG, e.toString() + "\n" + Log.getStackTraceString(new Throwable()));
            }
        }
        return defaultValue;
    }

    /**
     * Gets the currently active preset for a HA device
     *
     * @param device is the device for which we want to set the active preset
     * @return active preset index
     * @hide
     */
    @RequiresBluetoothConnectPermission
    @RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT)
    public boolean getActivePresetIndex(@NonNull BluetoothDevice device) {
        final IBluetoothHapClient service = getService();
        final boolean defaultValue = false;
        if (service == null) {
            Log.w(TAG, "Proxy not attached to service");
            if (DBG) log(Log.getStackTraceString(new Throwable()));
        } else if (isEnabled() && isValidDevice(device)) {
            try {
                final SynchronousResultReceiver<Boolean> recv = new SynchronousResultReceiver();
                service.getActivePresetIndex(device, mAttributionSource, recv);
                return recv.awaitResultNoInterrupt(getSyncTimeout()).getValue(defaultValue);
            } catch (RemoteException | TimeoutException e) {
                Log.e(TAG, e.toString() + "\n" + Log.getStackTraceString(new Throwable()));
            }
        }
        return defaultValue;
    }

    /**
     * Selects the currently active preset for a HA device
     *
     * @param device is the device for which we want to set the active preset
     * @param presetIndex is an index of one of the available presets
     * @return true if valid request was sent, false otherwise
     * @hide
     */
    @RequiresBluetoothConnectPermission
    @RequiresPermission(allOf = { android.Manifest.permission.BLUETOOTH_CONNECT,
            android.Manifest.permission.BLUETOOTH_PRIVILEGED })
    public boolean selectActivePreset(@NonNull BluetoothDevice device, int presetIndex) {
        final IBluetoothHapClient service = getService();
        final boolean defaultValue = false;
        if (service == null) {
            Log.w(TAG, "Proxy not attached to service");
            if (DBG) log(Log.getStackTraceString(new Throwable()));
        } else if (isEnabled() && isValidDevice(device)) {
            try {
                final SynchronousResultReceiver<Boolean> recv = new SynchronousResultReceiver();
                service.selectActivePreset(device, presetIndex, mAttributionSource, recv);
                return recv.awaitResultNoInterrupt(getSyncTimeout()).getValue(defaultValue);
            } catch (RemoteException | TimeoutException e) {
                Log.e(TAG, e.toString() + "\n" + Log.getStackTraceString(new Throwable()));
            }
        }
        return defaultValue;
    }

    /**
     * Selects the currently active preset for a HA device group.
     *
     * <p> This group call may replace multiple device calls if those are part of the
     * valid HAS group. Note that binaural HA devices may or may not support group.
     *
     * @param groupId is the device group identifier for which want to set the active preset
     * @param presetIndex is an index of one of the available presets
     * @return true if valid group request was sent, false otherwise
     * @hide
     */
    @RequiresBluetoothConnectPermission
    @RequiresPermission(allOf = { android.Manifest.permission.BLUETOOTH_CONNECT,
            android.Manifest.permission.BLUETOOTH_PRIVILEGED })
    public boolean groupSelectActivePreset(int groupId, int presetIndex) {
        final IBluetoothHapClient service = getService();
        final boolean defaultValue = false;
        if (service == null) {
            Log.w(TAG, "Proxy not attached to service");
            if (DBG) log(Log.getStackTraceString(new Throwable()));
        } else if (isEnabled()) {
            try {
                final SynchronousResultReceiver<Boolean> recv = new SynchronousResultReceiver();
                service.groupSelectActivePreset(groupId, presetIndex, mAttributionSource, recv);
                return recv.awaitResultNoInterrupt(getSyncTimeout()).getValue(defaultValue);
            } catch (RemoteException | TimeoutException e) {
                Log.e(TAG, e.toString() + "\n" + Log.getStackTraceString(new Throwable()));
            }
        }
        return defaultValue;
    }

    /**
     * Sets the next preset as a currently active preset for a HA device
     *
     * <p> Note that the meaning of 'next' is HA device implementation specific and
     * does not necessarily mean a higher preset index.
     *
     * @param device is the device for which we want to set the active preset
     * @return true if valid request was sent, false otherwise
     * @hide
     */
    @RequiresBluetoothConnectPermission
    @RequiresPermission(allOf = { android.Manifest.permission.BLUETOOTH_CONNECT,
            android.Manifest.permission.BLUETOOTH_PRIVILEGED })
    public boolean nextActivePreset(@NonNull BluetoothDevice device) {
        final IBluetoothHapClient service = getService();
        final boolean defaultValue = false;
        if (service == null) {
            Log.w(TAG, "Proxy not attached to service");
            if (DBG) log(Log.getStackTraceString(new Throwable()));
        } else if (isEnabled() && isValidDevice(device)) {
            try {
                final SynchronousResultReceiver<Boolean> recv = new SynchronousResultReceiver();
                service.nextActivePreset(device, mAttributionSource, recv);
                return recv.awaitResultNoInterrupt(getSyncTimeout()).getValue(defaultValue);
            } catch (RemoteException | TimeoutException e) {
                Log.e(TAG, e.toString() + "\n" + Log.getStackTraceString(new Throwable()));
            }
        }
        return defaultValue;
    }

    /**
     * Sets the next preset as a currently active preset for a HA device group
     *
     * <p> Note that the meaning of 'next' is HA device implementation specific and
     * does not necessarily mean a higher preset index.
     * <p> This group call may replace multiple device calls if those are part of the
     * valid HAS group. Note that binaural HA devices may or may not support group.
     *
     * @param groupId is the device group identifier for which want to set the active preset
     * @return true if valid group request was sent, false otherwise
     * @hide
     */
    @RequiresBluetoothConnectPermission
    @RequiresPermission(allOf = { android.Manifest.permission.BLUETOOTH_CONNECT,
            android.Manifest.permission.BLUETOOTH_PRIVILEGED })
    public boolean groupNextActivePreset(int groupId) {
        final IBluetoothHapClient service = getService();
        final boolean defaultValue = false;
        if (service == null) {
            Log.w(TAG, "Proxy not attached to service");
            if (DBG) log(Log.getStackTraceString(new Throwable()));
        } else if (isEnabled()) {
            try {
                final SynchronousResultReceiver<Boolean> recv = new SynchronousResultReceiver();
                service.groupNextActivePreset(groupId, mAttributionSource, recv);
                return recv.awaitResultNoInterrupt(getSyncTimeout()).getValue(defaultValue);
            } catch (RemoteException | TimeoutException e) {
                Log.e(TAG, e.toString() + "\n" + Log.getStackTraceString(new Throwable()));
            }
        }
        return defaultValue;
    }

    /**
     * Sets the previous preset as a currently active preset for a HA device.
     *
     * <p> Note that the meaning of 'previous' is HA device implementation specific and
     * does not necessarily mean a lower preset index.
     *
     * @param device is the device for which we want to set the active preset
     * @return true if valid request was sent, false otherwise
     * @hide
     */
    @RequiresBluetoothConnectPermission
    @RequiresPermission(allOf = { android.Manifest.permission.BLUETOOTH_CONNECT,
            android.Manifest.permission.BLUETOOTH_PRIVILEGED })
    public boolean previousActivePreset(@NonNull BluetoothDevice device) {
        final IBluetoothHapClient service = getService();
        final boolean defaultValue = false;
        if (service == null) {
            Log.w(TAG, "Proxy not attached to service");
            if (DBG) log(Log.getStackTraceString(new Throwable()));
        } else if (isEnabled() && isValidDevice(device)) {
            try {
                final SynchronousResultReceiver<Boolean> recv = new SynchronousResultReceiver();
                service.previousActivePreset(device, mAttributionSource, recv);
                return recv.awaitResultNoInterrupt(getSyncTimeout()).getValue(defaultValue);
            } catch (RemoteException | TimeoutException e) {
                Log.e(TAG, e.toString() + "\n" + Log.getStackTraceString(new Throwable()));
            }
        }
        return defaultValue;
    }

    /**
     * Sets the previous preset as a currently active preset for a HA device group
     *
     * <p> Note the meaning of 'previous' is HA device implementation specific and
     * does not necessarily mean a lower preset index.
     * <p> This group call may replace multiple device calls if those are part of the
     * valid HAS group. Note that binaural HA devices may or may not support group.
     *
     * @param groupId is the device group identifier for which want to set the active preset
     * @return true if valid group request was sent, false otherwise
     * @hide
     */
    @RequiresBluetoothConnectPermission
    @RequiresPermission(allOf = { android.Manifest.permission.BLUETOOTH_CONNECT,
            android.Manifest.permission.BLUETOOTH_PRIVILEGED })
    public boolean groupPreviousActivePreset(int groupId) {
        final IBluetoothHapClient service = getService();
        final boolean defaultValue = false;
        if (service == null) {
            Log.w(TAG, "Proxy not attached to service");
            if (DBG) log(Log.getStackTraceString(new Throwable()));
        } else if (isEnabled()) {
            try {
                final SynchronousResultReceiver<Boolean> recv = new SynchronousResultReceiver();
                service.groupPreviousActivePreset(groupId, mAttributionSource, recv);
                return recv.awaitResultNoInterrupt(getSyncTimeout()).getValue(defaultValue);
            } catch (RemoteException | TimeoutException e) {
                Log.e(TAG, e.toString() + "\n" + Log.getStackTraceString(new Throwable()));
            }
        }
        return defaultValue;
    }

    /**
     * Requests the preset info
     *
     * @param device is the device for which we want to get the preset name
     * @param presetIndex is an index of one of the available presets
     * @return true if valid request was sent, false otherwise
     * @hide
     */
    @RequiresBluetoothConnectPermission
    @RequiresPermission(allOf = { android.Manifest.permission.BLUETOOTH_CONNECT,
            android.Manifest.permission.BLUETOOTH_PRIVILEGED })
    public boolean getPresetInfo(@NonNull BluetoothDevice device, int presetIndex) {
        final IBluetoothHapClient service = getService();
        final boolean defaultValue = false;
        if (service == null) {
            Log.w(TAG, "Proxy not attached to service");
            if (DBG) log(Log.getStackTraceString(new Throwable()));
        } else if (isEnabled() && isValidDevice(device)) {
            try {
                final SynchronousResultReceiver<Boolean> recv = new SynchronousResultReceiver();
                service.getPresetInfo(device, presetIndex, mAttributionSource, recv);
                return recv.awaitResultNoInterrupt(getSyncTimeout()).getValue(defaultValue);
            } catch (RemoteException | TimeoutException e) {
                Log.e(TAG, e.toString() + "\n" + Log.getStackTraceString(new Throwable()));
            }
        }
        return defaultValue;
    }

    /**
     * Requests all presets info
     *
     * @param device is the device for which we want to get all presets info
     * @return true if request was processed, false otherwise
     * @hide
     */
    @RequiresBluetoothConnectPermission
    @RequiresPermission(allOf = { android.Manifest.permission.BLUETOOTH_CONNECT,
            android.Manifest.permission.BLUETOOTH_PRIVILEGED })
    public boolean getAllPresetsInfo(@NonNull BluetoothDevice device) {
        final IBluetoothHapClient service = getService();
        final boolean defaultValue = false;
        if (service == null) {
            Log.w(TAG, "Proxy not attached to service");
            if (DBG) log(Log.getStackTraceString(new Throwable()));
        } else if (isEnabled() && isValidDevice(device)) {
            try {
                final SynchronousResultReceiver<Boolean> recv = new SynchronousResultReceiver();
                service.getAllPresetsInfo(device, mAttributionSource, recv);
                return recv.awaitResultNoInterrupt(getSyncTimeout()).getValue(defaultValue);
            } catch (RemoteException | TimeoutException e) {
                Log.e(TAG, e.toString() + "\n" + Log.getStackTraceString(new Throwable()));
            }
        }
        return defaultValue;
    }

    /**
     * Requests HAP features
     *
     * @param device is the device for which we want to get features for
     * @return true if request was processed, false otherwise
     * @hide
     */
    @RequiresBluetoothConnectPermission
    @RequiresPermission(allOf = { android.Manifest.permission.BLUETOOTH_CONNECT,
            android.Manifest.permission.BLUETOOTH_PRIVILEGED })
    public boolean getFeatures(@NonNull BluetoothDevice device) {
        final IBluetoothHapClient service = getService();
        final boolean defaultValue = false;
        if (service == null) {
            Log.w(TAG, "Proxy not attached to service");
            if (DBG) log(Log.getStackTraceString(new Throwable()));
        } else if (isEnabled() && isValidDevice(device)) {
            try {
                final SynchronousResultReceiver<Boolean> recv = new SynchronousResultReceiver();
                service.getFeatures(device, mAttributionSource, recv);
                return recv.awaitResultNoInterrupt(getSyncTimeout()).getValue(defaultValue);
            } catch (RemoteException | TimeoutException e) {
                Log.e(TAG, e.toString() + "\n" + Log.getStackTraceString(new Throwable()));
            }
        }
        return defaultValue;
    }

    /**
     * Sets the preset name
     *
     * <p> Note that the name length is restricted to 30 characters.
     *
     * @param device is the device for which we want to get the preset name
     * @param presetIndex is an index of one of the available presets
     * @param name is a new name for a preset
     * @return true if valid request was sent, false otherwise
     * @hide
     */
    @RequiresBluetoothConnectPermission
    @RequiresPermission(allOf = { android.Manifest.permission.BLUETOOTH_CONNECT,
            android.Manifest.permission.BLUETOOTH_PRIVILEGED })
    public boolean setPresetName(@NonNull BluetoothDevice device, int presetIndex,
                                 @NonNull String name) {
        final IBluetoothHapClient service = getService();
        final boolean defaultValue = false;
        if (service == null) {
            Log.w(TAG, "Proxy not attached to service");
            if (DBG) log(Log.getStackTraceString(new Throwable()));
        } else if (isEnabled() && isValidDevice(device)) {
            try {
                final SynchronousResultReceiver<Boolean> recv = new SynchronousResultReceiver();
                service.setPresetName(device, presetIndex, name, mAttributionSource, recv);
                return recv.awaitResultNoInterrupt(getSyncTimeout()).getValue(defaultValue);
            } catch (RemoteException | TimeoutException e) {
                Log.e(TAG, e.toString() + "\n" + Log.getStackTraceString(new Throwable()));
            }
        }
        return defaultValue;
    }

    /**
     * Sets the preset name
     *
     * <p> Note that the name length is restricted to 30 characters.
     *
     * @param groupId is the device group identifier
     * @param presetIndex is an index of one of the available presets
     * @param name is a new name for a preset
     * @return true if valid request was sent, false otherwise
     * @hide
     */
    @RequiresBluetoothConnectPermission
    @RequiresPermission(allOf = { android.Manifest.permission.BLUETOOTH_CONNECT,
            android.Manifest.permission.BLUETOOTH_PRIVILEGED })
    public boolean groupSetPresetName(int groupId, int presetIndex, @NonNull String name) {
        final IBluetoothHapClient service = getService();
        final boolean defaultValue = false;
        if (service == null) {
            Log.w(TAG, "Proxy not attached to service");
            if (DBG) log(Log.getStackTraceString(new Throwable()));
        } else if (isEnabled()) {
            try {
                final SynchronousResultReceiver<Boolean> recv = new SynchronousResultReceiver();
                service.groupSetPresetName(groupId, presetIndex, name, mAttributionSource, recv);
                return recv.awaitResultNoInterrupt(getSyncTimeout()).getValue(defaultValue);
            } catch (RemoteException | TimeoutException e) {
                Log.e(TAG, e.toString() + "\n" + Log.getStackTraceString(new Throwable()));
            }
        }
        return defaultValue;
    }

    private boolean isEnabled() {
        if (mAdapter.getState() == BluetoothAdapter.STATE_ON) return true;
        return false;
    }

    private boolean isValidDevice(BluetoothDevice device) {
        if (device == null) return false;

        if (BluetoothAdapter.checkBluetoothAddress(device.getAddress())) return true;
        return false;
    }

    private static void log(String msg) {
        Log.d(TAG, msg);
    }
}