
package com.dlcmodbus;

import android.util.Log;
import android.serialport.SerialPortFinder;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.WritableMap;

import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.WritableArray;
import com.licheedev.modbus4android.ModbusCallback;
import com.licheedev.modbus4android.ModbusParam;
import com.licheedev.modbus4android.param.SerialParam;
import com.dlcmodbus.modbus.ByteUtil;
import com.dlcmodbus.modbus.ModbusManager;
import com.serotonin.modbus4j.ModbusMaster;
import com.serotonin.modbus4j.msg.ReadHoldingRegistersResponse;
import com.serotonin.modbus4j.msg.WriteRegistersResponse;
import com.serotonin.modbus4j.msg.WriteRegisterResponse;

import static android.content.ContentValues.TAG;

public class RNModbusModule extends ReactContextBaseJavaModule {

  private final ReactApplicationContext reactContext;

  public RNModbusModule(ReactApplicationContext reactContext) {
    super(reactContext);
    this.reactContext = reactContext;
  }

  @Override
  public String getName() {
    return "RNModbus";
  }

//获取端口列表
@ReactMethod
public void getAllDevicesPath(Promise promise){
  try {
    SerialPortFinder serialPortFinder = new SerialPortFinder();
  String[] paths = serialPortFinder.getAllDevicesPath();
  WritableArray pathArray = Arguments.createArray();
  for (int i = 0; i <paths.length ; i++) {
    String path = paths[i];
    pathArray.pushString(path);
  }
  // WritableMap map = Arguments.createMap();
  // map.putArray("data",pathArray);
  promise.resolve(pathArray);
  } catch (Exception e) {
    //TODO: handle exception
    promise.reject("E_LAYOUT_ERROR", e);
  }

}


  /**
   * 打开ModBlus
   * rn调用Native,并获取返回值
   * @param path 串口路径
   * @param baudrate 波特率
   * @param dataBits 数据位
   * @param parity 校验位
   * @param stopBits 停止位
   */
  @ReactMethod
  public void openDevice(String path, int baudrate, int dataBits, int parity,int stopBits,final Promise promise) {
    if (ModbusManager.get().isModbusOpened()) {
      // 关闭设备
      ModbusManager.get().closeModbusMaster();
      return;
    }

    // 串口
    ModbusParam serialParam =
            SerialParam.create(path, baudrate).setDataBits(dataBits) // 数据位
                    .setParity(parity) // 校验位
                    .setStopBits(stopBits) // 停止位
                    .setTimeout(1000).setRetries(0); // 不重试 // 不重试
    ModbusManager.get().closeModbusMaster();
    ModbusManager.get().init(serialParam, new ModbusCallback<ModbusMaster>() {
      @Override
      public void onSuccess(ModbusMaster modbusMaster) {
        Log.d(TAG, "onSuccess: 打开成功");
          WritableMap map = Arguments.createMap();
          map.putDouble("code", 1);
          promise.resolve(map);
      }

      @Override
      public void onFailure(Throwable tr) {
        Log.d(TAG, "onFailure: 打开失败");
          WritableMap map = Arguments.createMap();
          map.putDouble("code", 0);
          promise.resolve(map);

      }

      @Override
      public void onFinally() {

      }

    });
  }

  @ReactMethod
  public  void  readHoldingRegisters(final int slaveId, final int start, final int len, final Promise promise ){

    ModbusManager.get()
            .readHoldingRegisters(slaveId, start, len,
                    new ModbusCallback<ReadHoldingRegistersResponse>() {
                      @Override
                      public void onSuccess(
                              ReadHoldingRegistersResponse readHoldingRegistersResponse) {
                        //short[] shortData = readHoldingRegistersResponse.getShortData();
                        byte[] data = readHoldingRegistersResponse.getData();
                        Log.d(TAG, "onSuccess: 读到数据:"+ ByteUtil.bytes2HexStr(data));
                        WritableArray receiveArray = Arguments.createArray();
                        for (int i = 0; i < data.length; i++) {
                          // int cmdSingle = data[i] & 0xFF;
                          // receiveArray.pushInt(cmdSingle);
                          String cmdSingle = Integer.toHexString(data[i] & 0xFF);
                          receiveArray.pushString(cmdSingle);
                        }
//                          promise.resolve(receiveArray);
                          WritableMap map = Arguments.createMap();
                          map.putDouble("code", 1);
                          map.putArray("data",receiveArray);
                          promise.resolve(map);
                      }

                      @Override
                      public void onFailure(Throwable tr) {
                        Log.d(TAG, "onSuccess: 读到数据:"+ 12346);
                          WritableArray receiveArray = Arguments.createArray();
//                          promise.resolve(receiveArray);
                          WritableMap map = Arguments.createMap();
                          map.putDouble("code", 0);
                          map.putArray("data",receiveArray);
                          promise.resolve(map);

                      }

                      @Override
                      public void onFinally() {

                      }
                    });
  }

  @ReactMethod
  public  void writeRegisters(final int slaveId, final int start, ReadableArray values, final Promise promise){
    Log.d("BBC", "BBC: " +     values.toString());
    int length = values.size();
    short[] cmd = new short[length];
    for (int i = 0; i < length; i++) {
      int number =  values.getInt(i);
      cmd[i] =(short)number ;
    }
    ModbusManager.get()
            .writeRegisters(slaveId, start, cmd,
                    new ModbusCallback<WriteRegistersResponse>() {
                      @Override
                      public void onSuccess(WriteRegistersResponse writeRegistersResponse) {
                        // 发送成功
                          WritableArray receiveArray = Arguments.createArray();

                          WritableMap map = Arguments.createMap();
                          map.putDouble("code", 1);
                          map.putArray("data",receiveArray);
                          promise.resolve(map);

                      }

                      @Override
                      public void onFailure(Throwable tr) {
                        Log.d("BBC", "BAAAABC: ");
                          WritableArray receiveArray = Arguments.createArray();

                          WritableMap map = Arguments.createMap();
                          map.putDouble("code", 0);
                          map.putArray("data",receiveArray);
                          promise.resolve(map);
                      }

                      @Override
                      public void onFinally() {

                      }
                    });
  }


  // 写单个寄存器
  @ReactMethod
  public  void writeSingleRegister(final int slaveId, final int start, final int value, final Promise promise){

    ModbusManager.get()
            .writeSingleRegister(slaveId, start, value,
                    new ModbusCallback<WriteRegisterResponse>() {
                      @Override
                      public void onSuccess(WriteRegisterResponse writeRegistersResponse) {
                        // 发送成功
//                          promise.resolve(1);
                          WritableArray receiveArray = Arguments.createArray();

                          WritableMap map = Arguments.createMap();
                          map.putDouble("code", 1);
                          map.putArray("data",receiveArray);
                          promise.resolve(map);

                      }

                      @Override
                      public void onFailure(Throwable tr) {
                        Log.d("BBC", "BAAAABC: ");
                          WritableArray receiveArray = Arguments.createArray();

                          WritableMap map = Arguments.createMap();
                          map.putDouble("code", 0);
                          map.putArray("data",receiveArray);
                          promise.resolve(map);

                      }

                      @Override
                      public void onFinally() {

                      }
                    });
  }



}
