package org.udbuilder.remotecontroller.transfer;

import android.os.Bundle;

/**
 * Created by xiongjianbo on 2018/1/25.
 */

public interface TransferListener {
    void transferStart(Bundle bundle);
    void transferError(Bundle bundle);
    void transferCompleted(Bundle bundle);
}
