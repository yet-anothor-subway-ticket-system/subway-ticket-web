package com.subwayticket.util;

import com.google.gson.Gson;
import com.subwayticket.database.model.Account;
import com.subwayticket.model.MobileToken;
import com.subwayticket.model.PhoneCaptcha;
import com.subwayticket.model.PublicResultCode;
import com.subwayticket.model.result.Result;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.impl.crypto.MacProvider;
import redis.clients.jedis.Jedis;

import javax.servlet.ServletRequest;
import java.security.Key;
import java.util.Date;

/**
 * @author buptsse-zero <GGGZ-1101-28@Live.cn>
 */
public class SecurityUtil {
    public static final String REDIS_KEY_PHONE_CAPTCHA_PREFIX = "PhoneCaptcha";
    public static final String REDIS_KEY_PHONE_CAPTCHA_ERROR_TIMES_PREFIX = "PhoneCaptchaErrorTimes";
    public static final int PHONE_CAPTCHA_VALID_MINUTES = 30;
    public static final int PHONE_CAPTCHA_INTERVAL_SECONDS = 60;
    public static final int PHONE_CAPTCHA_ERROR_TIMES = 3;

    public static final String REDIS_KEY_MOBILETOKEN = "MobileToken";
    public static int MOBILETOKEN_VALID_HOURS = 120;

    public static Result sendPhoneCaptcha(ServletRequest request, String phoneNumber, Jedis jedis){
        if(phoneNumber == null || phoneNumber.isEmpty())
            return new Result(PublicResultCode.PHONE_CAPTCHA_SEND_FAILED, BundleUtil.getString(request, "TipCaptchaSendFailed"));
        PhoneCaptcha pc = PhoneCaptcha.fromString(jedis.get(REDIS_KEY_PHONE_CAPTCHA_PREFIX + phoneNumber));
        if(pc != null){
            if(new Date().getTime() - pc.getSendTime().getTime() < PHONE_CAPTCHA_INTERVAL_SECONDS * 1000)
                return new Result(PublicResultCode.PHONE_CAPTCHA_INTERVAL_ILLEGAL, BundleUtil.getString(request, "TipCaptchaIntervalIllegal"));
        }
        jedis.setex(REDIS_KEY_PHONE_CAPTCHA_PREFIX + phoneNumber, PHONE_CAPTCHA_VALID_MINUTES * 60, new PhoneCaptcha("123456").toString());
        jedis.setex(REDIS_KEY_PHONE_CAPTCHA_ERROR_TIMES_PREFIX + phoneNumber, PHONE_CAPTCHA_VALID_MINUTES * 60, Integer.toString(0));
        return new Result(PublicResultCode.SUCCESS, BundleUtil.getString(request, "TipCaptchaSendSuccess"));
    }

    public static boolean checkPhoneCaptcha(String phoneNumber, String captcha, Jedis jedis){
        if(phoneNumber == null || captcha == null)
            return false;
        PhoneCaptcha pc = PhoneCaptcha.fromString(jedis.get(REDIS_KEY_PHONE_CAPTCHA_PREFIX + phoneNumber));
        if(pc == null || !captcha.equals(pc.getCode()))
            return false;
        return true;
    }

    public static boolean increasePhoneCaptchaErrorTimes(Jedis jedis, String phoneNumber){
        String errorTimesStr = jedis.get(REDIS_KEY_PHONE_CAPTCHA_ERROR_TIMES_PREFIX + phoneNumber);
        if(errorTimesStr == null)
            return false;
        int errorTimes = Integer.valueOf(errorTimesStr);
        errorTimes++;
        jedis.set(REDIS_KEY_PHONE_CAPTCHA_ERROR_TIMES_PREFIX + phoneNumber, Integer.toString(errorTimes));
        if(errorTimes >= PHONE_CAPTCHA_ERROR_TIMES)
            return true;
        return false;
    }

    public static void clearPhoneCaptcha(Jedis jedis, String phoneNumber){
        jedis.del(REDIS_KEY_PHONE_CAPTCHA_PREFIX + phoneNumber);
        jedis.del(REDIS_KEY_PHONE_CAPTCHA_ERROR_TIMES_PREFIX + phoneNumber);
    }

    private static Key tokenkey = MacProvider.generateKey();
    public static final String HEADER_TOKEN_KEY = "AuthToken";

    public static String getMobileToken(String phoneNumber){
        MobileToken token = new MobileToken(phoneNumber);
        Gson gson = GsonUtil.getGson();
        String tokenString = Jwts.builder().setIssuer(phoneNumber).setSubject(gson.toJson(token)).signWith(SignatureAlgorithm.HS512, tokenkey).compact();
        return tokenString;
    }

    public static Account checkMobileToken(String tokenStr, Jedis jedis){
        if(tokenStr == null)
            return null;
        try {
            String subject = Jwts.parser().setSigningKey(tokenkey).parseClaimsJws(tokenStr).getBody().getSubject();
            MobileToken token = GsonUtil.getGson().fromJson(subject, MobileToken.class);
            String redisToken = jedis.get(REDIS_KEY_MOBILETOKEN + token.getPhoneNumber());
            if(!tokenStr.equals(redisToken))
                return null;
            jedis.setex(REDIS_KEY_MOBILETOKEN + token.getPhoneNumber(), MOBILETOKEN_VALID_HOURS * 3600, redisToken);
            return new Account(token.getPhoneNumber());
        }catch (Exception e){
            e.printStackTrace();
            return null;
        }
    }
}
