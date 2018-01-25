package org.udbuilder.remotecontroller.transfer;

import android.os.Handler;

/**
 * Created by xiongjianbo on 2018/1/25.
 */

public class Transfer {
    TransferListener mListener;

    Handler mHandler;

    public Transfer(TransferListener listener) {
        mListener = listener;
    }
}
