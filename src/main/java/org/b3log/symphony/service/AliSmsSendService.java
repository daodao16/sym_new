package org.b3log.symphony.service;

import org.b3log.latke.logging.Level;
import org.b3log.latke.logging.Logger;
import org.b3log.latke.service.ServiceException;
import org.b3log.latke.service.annotation.Service;
import org.b3log.symphony.util.Symphonys;
import org.json.JSONObject;

import com.aliyuncs.CommonRequest;
import com.aliyuncs.CommonResponse;
import com.aliyuncs.DefaultAcsClient;
import com.aliyuncs.IAcsClient;
import com.aliyuncs.http.MethodType;
import com.aliyuncs.profile.DefaultProfile;

@Service
public class AliSmsSendService {

	/**
	 * Logger.
	 */
	private static final Logger LOGGER = Logger
			.getLogger(AliSmsSendService.class);

	// 产品版本:云通信短信API产品版本,开发者无需替换
	private static final String version = "2017-05-25";
	// 产品域名,开发者无需替换
	private static final String domain = "dysmsapi.aliyuncs.com";
	// 产品操作,开发者无需替换
	private static final String action = "SendSms";

	private String signName = "loser联盟";

	/**
	 * send sms by aliyun synchronous
	 * @param mobile
	 * @param tempCode
	 * @param params
	 * @throws ServiceException
	 */
	public void send(String mobile, String tempCode, JSONObject params)
			throws ServiceException {
		if (LOGGER.isInfoEnabled()) {
			LOGGER.info("Ali sms service send message:mobile=" + mobile
					+ "tempCode=" + tempCode + ",params=" + params);
		}
		String accessKeyId = Symphonys.get("sms.aliyun.accessKeyId");
		String accessKeySecret = Symphonys.get("sms.aliyun.accessKeySecret");
		// String accessKeyId = "LTAIHxvidTlcy8Z6";
		// String accessKeySecret = "JS75sCMC3QK2OkujO96yingW2uA6jd";
		// 初始化acsClient,暂不支持region化
		try {
			DefaultProfile profile = DefaultProfile.getProfile("cn-hangzhou",
					accessKeyId, accessKeySecret);
			IAcsClient client = new DefaultAcsClient(profile);

			CommonRequest request = new CommonRequest();
			// request.setProtocol(ProtocolType.HTTPS);
			request.setMethod(MethodType.POST);
			request.setDomain(domain);
			request.setVersion(version);
			request.setAction(action);

			// 必填:待发送手机号
			request.putQueryParameter("PhoneNumbers", mobile);
			// 必填:短信签名-可在短信控制台中找到
			request.putQueryParameter("SignName", signName);
			// 必填:短信模板-可在短信控制台中找到
			request.putQueryParameter("TemplateCode", tempCode);
			// 可选:模板中的变量替换JSON串,如模板内容为"亲爱的${name},您的验证码为${code}"时,此处的值为
			request.putQueryParameter("TemplateParam", params.toString());

			// hint 此处可能会抛出异常，注意catch
			CommonResponse response = client.getCommonResponse(request);
			JSONObject result = new JSONObject(response.getData());
			if (LOGGER.isInfoEnabled()) {
				LOGGER.log(Level.INFO,
						"Ali sms service send message response message:",
						result.optString("Message"));
			}
			if (result.optString("Code") == null
					|| !result.optString("Code").equals("OK")) {// 发送失败
				LOGGER.log(Level.ERROR,
						"AliSmsSendService send error,response message:",
						result.optString("Message"));
				throw new ServiceException(result.optString("Message"));
			}
		} catch (Exception e) {
			LOGGER.log(Level.ERROR, "AliSmsSendService", e);
			throw new ServiceException(e);
		}
	}
	
	/**
	 * send sms by aliyun asynchronous
	 * @param mobile
	 * @param tempCode
	 * @param params
	 */
	public void sendAsyn(String mobile, String tempCode, JSONObject params) {
		Symphonys.EXECUTOR_SERVICE
				.submit(() -> {
					try {
						send(mobile, tempCode, params);
					} catch (ServiceException e) {
						LOGGER.log(Level.ERROR,
								"AliSmsSendService Asyn send error:", e);
					}
				});
	}

	public static void main(String[] args) {
		AliSmsSendService aliSmsSendService = new AliSmsSendService();
		JSONObject params = new JSONObject();
		params.put("code", "123456");
		try {
			aliSmsSendService.send("13588010870", "SMS_135038442", params);
		} catch (ServiceException e) {
			e.printStackTrace();
		}

	}

}
