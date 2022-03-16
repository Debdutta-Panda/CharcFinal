package com.vxplore.charc.tokener.sample;


import com.vxplore.charc.tokener.rtm.RtmTokenBuilder;

public class RtmTokenBuilderSample {
    private static String appId = "a47b50dab04949b7b9ee95b05902b014";
    private static String appCertificate = "4b2f8c4f885c40a7b4f25793231c35da";
    private static String userId = "2882341273";
    private static int expireTimestamp = 0;

    public static void main(String[] args) throws Exception {
    	RtmTokenBuilder token = new RtmTokenBuilder();
        String result = token.buildToken(appId, appCertificate, userId, RtmTokenBuilder.Role.Rtm_User, expireTimestamp);
        System.out.println(result);
    }
}
