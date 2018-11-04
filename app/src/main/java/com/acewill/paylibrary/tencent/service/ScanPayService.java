package com.acewill.paylibrary.tencent.service;

import com.acewill.paylibrary.tencent.common.Configure;
import com.acewill.paylibrary.tencent.protocol.pay_protocol.ScanPayReqData;

/**
 * User: rizenguo
 * Date: 2014/10/29
 * Time: 16:03
 */
public class ScanPayService extends BaseService {

    public ScanPayService() throws IllegalAccessException, InstantiationException, ClassNotFoundException {
        super(Configure.PAY_API);
    }

    /**
     * 请求支付服务
     * @param scanPayReqData 这个数据对象里面包含了API要求提交的各种数据字段
     * @return API返回的数据
     * @throws Exception
     */
    // 刷卡支付（反扫）
    public String request(ScanPayReqData scanPayReqData) throws Exception {

        //--------------------------------------------------------------------
        //发送HTTPS的Post请求到API地址
        //--------------------------------------------------------------------
        String responseString = sendPost(scanPayReqData);

        return responseString;
    }
}
