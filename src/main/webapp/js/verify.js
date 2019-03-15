/*
 * Symphony - A modern community (forum/BBS/SNS/blog) platform written in Java.
 * Copyright (C) 2012-2019, b3log.org & hacpai.com
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
/**
 * @fileoverview register.
 *
 * @author <a href="http://vanessa.b3log.org">Liyuan Li</a>
 * @author <a href="http://88250.b3log.org">Liang Ding</a>
 * @version 2.8.0.0, Jan 7, 2018
 */

/**
 * @description Verify
 * @static
 */
var Verify = {
    /**
     * @description 登录
     */
    login: function (goto) {
        if (Validate.goValidate({target: $('#loginTip'),
            data: [{
                    "target": $("#nameOrMobile"),
                    "type": "string",
                    "max": 256,
                    "msg": Label.loginNameErrorLabel
                }]})) {
            var requestJSONObject = {
                nameOrMobile: $("#nameOrMobile").val().replace(/(^\s*)|(\s*$)/g, ""),
                userPassword: calcMD5($("#loginPassword").val()),
                rememberLogin: $("#rememberLogin").prop("checked"),
                captcha: $('#captchaLogin').val().replace(/(^\s*)|(\s*$)/g, "")
            };

            $.ajax({
                url: Label.servePath + "/login",
                type: "POST",
                cache: false,
                data: JSON.stringify(requestJSONObject),
                success: function (result, textStatus) {
                    if (result.sc) {
                        window.location.href = goto;
                    } else {
                        $("#loginTip").addClass('error').html('<ul><li>' + result.msg + '</li></ul>');

                        if (result.needCaptcha && "" !== result.needCaptcha) {
                            $('#captchaImg').parent().show();
                            $("#captchaImg").attr("src", Label.servePath + "/captcha/login?needCaptcha="
                                    + result.needCaptcha + "&t=" + Math.random())
                                    .click(function () {
                                        $(this).attr('src', Label.servePath + "/captcha/login?needCaptcha="
                                                + result.needCaptcha + "&t=" + Math.random())
                                    });
                        }
                    }
                }
            });
        }
    },
    /**
     * @description Register sendMobileVertifyCode
     */
    sendMobileVCode: function (inputObj, tipObj, btnObj, captchaObj, captchaImgObj, bizType) {
    	if (Validate.goValidate({target: tipObj,
            data: [{
                    "target": inputObj,
                    "msg": Label.invalidUserMobileLabel,
                    "type": "mobile"
                }]})) {
            var requestJSONObject = {
                userMobile: inputObj.val().replace(/(^\s*)|(\s*$)/g, ""),
                captcha: captchaObj.val(),
                vertifyType: bizType
            };
            
            var clock = '';
	  		var nums = 60;

            btnObj.attr('disabled', true);

            $.ajax({
                url: Label.servePath + "/sendMobileVCode",
                type: "POST",
                cache: false,
                data: JSON.stringify(requestJSONObject),
                success: function (result, textStatus) {
                    if (result.sc) {
                        tipObj.addClass('succ').removeClass('error').html('<ul><li>' + result.msg + '</li></ul>');                     
	  					btnObj.text(nums+ Label.forbidMobileVCodeLabel);
     	  				clock = setInterval(doLoop, 1000);
                    } else {
                    	btnObj.attr('disabled', false);
                        tipObj.addClass('error').removeClass('succ').html('<ul><li>' + result.msg + '</li></ul>');
                        captchaImgObj.attr("src", Label.servePath + "/captcha?code=" + Math.random());
                        captchaObj.val("");
                    }
                }
            });
            
            function doLoop() {
            	nums--;
				if(nums > 0){
					btnObj.text(nums+ Label.forbidMobileVCodeLabel);
				}else{
					clearInterval(clock); //清除js定时器
					btnObj.attr('disabled', false);
					btnObj.text(Label.resendMobileVCodeLabel);
					captchaImgObj.attr("src", Label.servePath + "/captcha?code=" + Math.random());
            		captchaObj.val("");
					nums = 60; //重置时间
				}
            }
        }
    },
    
    /**
     * @description Register Step 1
     */
    register: function () {
        if (Validate.goValidate({target: $("#registerTip"),
            data: [{
                    "target": $("#registerUserName"),
                    "msg": Label.invalidUserNameLabel,
                    "type": 'string',
                    'max': 20
                }, {
                    "target": $("#registerUserMobile"),
                    "msg": Label.invalidUserMobileLabel,
                    "type": "mobile"
                }, {
                    "target": $("#registerVCode"),
                    "msg": Label.invalidVCodeLabel,
                    "type": 'string',
                    'max': 6
                }]})) {
            var requestJSONObject = {
                userName: $("#registerUserName").val().replace(/(^\s*)|(\s*$)/g, ""),
                userMobile: $("#registerUserMobile").val().replace(/(^\s*)|(\s*$)/g, ""),
                invitecode: $("#registerInviteCode").val().replace(/(^\s*)|(\s*$)/g, ""),
                captcha: $("#registerCaptcha").val(),
                userMobileVCode: $("#registerVCode").val().replace(/(^\s*)|(\s*$)/g, ""),
                referral: sessionStorage.r || ''
            };

            $("#registerBtn").attr('disabled', 'disabled');

            $.ajax({
                url: Label.servePath + "/register",
                type: "POST",
                cache: false,
                data: JSON.stringify(requestJSONObject),
                success: function (result, textStatus) {
                    if (result.sc) {
                        //$("#registerTip").addClass('succ').removeClass('error').html('<ul><li>' + result.msg + '</li></ul>');
                        //$("#registerBtn").attr('disabled', 'disabled');
                        var redirectURI =  Label.servePath + "/register?uid=" + result.userId;
                        if(sessionStorage.r) {
                        	redirectURI = redirectURI + "&r=" +sessionStorage.r;
                        }
                        window.location.href = redirectURI;
                    } else {
                        $("#registerTip").addClass('error').removeClass('succ').html('<ul><li>' + result.msg + '</li></ul>');
                        $("#registerCaptchaImg").attr("src", Label.servePath + "/captcha?code=" + Math.random());
                        $("#registerCaptcha").val("");
                        $("#registerBtn").removeAttr('disabled');
                    }
                }
            });
        }
    },
    /**
     * @description Register Step 2
     */
    register2: function () {
        if (Validate.goValidate({target: $("#registerTip2"),
            data: [{
                    "target": $("#registerUserPassword2"),
                    "msg": Label.invalidPasswordLabel,
                    "type": 'password',
                    'max': 20
                }, {
                    "target": $("#registerConfirmPassword2"),
                    "original": $("#registerUserPassword2"),
                    "msg": Label.confirmPwdErrorLabel,
                    "type": "confirmPassword"
                }]})) {
            var requestJSONObject = {
                //userAppRole: $("input[name=userAppRole]:checked").val(),
                userPassword: calcMD5($("#registerUserPassword2").val()),
                jobCode: $("#jobId").val(),
                referral: $("#referral2").val(),
                userId: $("#userId2").val()
            };

            $.ajax({
                url: Label.servePath + "/register2",
                type: "POST",
                cache: false,
                data: JSON.stringify(requestJSONObject),
                success: function (result, textStatus) {
                    if (result.sc) {
                        window.location.href = Label.servePath;
                    } else {
                        $("#registerTip2").addClass('error').removeClass('succ').html('<ul><li>' + result.msg + '</li></ul>');
                    }
                }
            });
        }
    },
    /**
     * @description guide leaveInfo 
     */
    leaveInfo: function () {
    	if (Validate.goValidate({target: $("#leaveTip"),
            data: [{
                    "target": $("#mainSkill"),
                    "msg": Label.invalidMainSkillLabel,
                    "type": 'string',
                    'max': 255
                }]})) {
            var requestJSONObject = {
                leaveType: $("input[name=leaveActive]:checked").val(),
                leavePeriod: $("#leavePeriod").val().replace(/(^\s*)|(\s*$)/g, ""),
                leaveReason: $("#leaveReason").val().replace(/(^\s*)|(\s*$)/g, ""),
                mainSkill: $("#mainSkill").val().replace(/(^\s*)|(\s*$)/g, "")                
            };

            $("#leaveInfoBtn").attr('disabled', 'disabled');

            $.ajax({
                url: Label.servePath + "/guide/updateJobInfo",
                type: "POST",
                cache: false,
                data: JSON.stringify(requestJSONObject),
                success: function (result, textStatus) {
                    if (result.sc) {
                        $("#leaveTip").addClass('succ').removeClass('error').html('<ul><li>' + result.msg + '</li></ul>');
                        $("#leaveInfoBtn").removeAttr('disabled');
                    } else {
                        $("#leaveTip").addClass('error').removeClass('succ').html('<ul><li>' + result.msg + '</li></ul>');
                        $("#leaveInfoBtn").removeAttr('disabled');
                    }
                }
            });
        }
    },
    /**
     * @description Forget password
     */
    forgetPwd: function () {
        if (Validate.goValidate({target: $("#fpwdTip"),
            data: [{
                    "target": $("#fpwdUserMobile"),
                    "msg": Label.invalidUserMobileLabel,
                    "type": "mobile"
                }, {
                    "target": $("#fpwdSecurityCode"),
                    "msg": Label.captchaErrorLabel,
                    "type": 'string',
                    'max': 4
                }, {
                    "target": $("#fpwdVCode"),
                    "msg": Label.invalidVCodeLabel,
                    "type": 'string',
                    'max': 6
                }]})) {
            var requestJSONObject = {
                userMobile: $("#fpwdUserMobile").val().replace(/(^\s*)|(\s*$)/g, ""),
                userMobileVCode: $("#fpwdVCode").val().replace(/(^\s*)|(\s*$)/g, ""),
                captcha: $("#fpwdSecurityCode").val()
            };

            $.ajax({
                url: Label.servePath + "/forget-pwd",
                type: "POST",
                cache: false,
                data: JSON.stringify(requestJSONObject),
                success: function (result, textStatus) {
                    if (result.sc) {
                        //$("#fpwdTip").addClass('succ').removeClass('error').html('<ul><li>' + result.msg + '</li></ul>');
                        var redirectURI =  Label.servePath + "/reset-pwd?code=" + $("#fpwdVCode").val().replace(/(^\s*)|(\s*$)/g, "");
                        
                        window.location.href = redirectURI;
                    } else {
                        $("#fpwdTip").removeClass("tip-succ");
                        $("#fpwdTip").addClass('error').removeClass('succ').html('<ul><li>' + result.msg + '</li></ul>');
                        $("#fpwdCaptcha").attr("src", Label.servePath + "/captcha?code=" + Math.random());
                        $("#fpwdSecurityCode").val("");
                    }
                }
            });
        }
    },
    /**
     * @description Reset password
     */
    resetPwd: function () {
        if (Validate.goValidate({target: $("#rpwdTip"),
            data: [{
                    "target": $("#rpwdUserPassword"),
                    "msg": Label.invalidPasswordLabel,
                    "type": 'password',
                    'max': 20
                }, {
                    "target": $("#rpwdConfirmPassword"),
                    "original": $("#rpwdUserPassword"),
                    "msg": Label.confirmPwdErrorLabel,
                    "type": "confirmPassword"
                }]})) {
            var requestJSONObject = {
                userPassword: calcMD5($("#rpwdUserPassword").val()),
                userId: $("#rpwdUserId").val(),
                code: $("#code").val()
            };

            $.ajax({
                url: Label.servePath + "/reset-pwd",
                type: "POST",
                cache: false,
                data: JSON.stringify(requestJSONObject),
                success: function (result, textStatus) {
                    if (result.sc) {
                        window.location.href = Label.servePath;
                    } else {
                        $("#rpwdTip").addClass('error').removeClass('succ').html('<ul><li>' + result.msg + '</li></ul>');
                    }
                }
            });
        }
    },
    /**
     * 登录注册等页面回车事件绑定
     */
    init: function () {
        // 注册回车事件
        $("#registerCaptcha, #registerInviteCode").keyup(function (event) {
            if (event.keyCode === 13) {
                Verify.register();
            }
        });

        // 忘记密码回车事件
        $("#fpwdSecurityCode").keyup(function (event) {
            if (event.keyCode === 13) {
                Verify.forgetPwd();
            }
        });

        // 登录密码输入框回车事件
        $("#loginPassword, #captchaLogin").keyup(function (event) {
            if (event.keyCode === 13) {
                $('#loginTip').next().click();
            }
        });

        // 重置密码输入框回车事件
        $("#rpwdConfirmPassword").keyup(function (event) {
            if (event.keyCode === 13) {
                Verify.resetPwd();
            }
        });
    },
    
    /**
     * 移动端输入框遮挡问题
     */
     initMobile: function () {
     	$("#jobDesc").focus(function () {
     		setTimeout(function(){
				document.body.scrollTop = document.body.scrollHeight;
			},300);
     	});
     },
    
    /**
     * 新手向导初始化
     * @param {int} currentStep 新手向导步骤，0 为向导完成
     * @param {int} tagSize 标签数
     */
    initGuide: function (currentStep, tagSize) {
        if (currentStep === 0) {
            window.location.href = Label.servePath;
            return false;
        }

        var step2Sort = 'random';

        var step = function () {
            if (currentStep !== 6) {
                $('.intro dt').removeClass('current');
                $('.guide-tab > div').hide();
            }

            if (currentStep < 6 && currentStep > 0) {
                $.ajax({
                    url: Label.servePath + "/guide/next",
                    type: "POST",
                    cache: false,
                    data: JSON.stringify({
                        userGuideStep: currentStep
                    }),
                    success: function (result, textStatus) {
                        if (!result.sc) {
                            Util.alert(result.msg);
                        }
                    }
                });
            }


            switch (currentStep) {
                case 1:
                    $('.guide-tab > div:eq(0)').show();

                    $('.step-btn .red').hide();

                    $('.intro dt:eq(0)').addClass('current');
                    break;
                case 2:
                    $('.guide-tab > div:eq(1)').show();

                    $('.step-btn .red').show();

                    $('.intro dt:eq(1)').addClass('current');

                    // sort
                    $('.step-btn .green, .step-btn .red').prop('disabled', true);
                    $('.tag-desc').isotope({
                        sortBy: step2Sort
                    });
                    step2Sort = (step2Sort === 'random' ? 'original-order' : 'random');
                    $('.tag-desc').on( 'arrangeComplete', function () {
                        $('.step-btn .green, .step-btn .red').prop('disabled', false);
                    });
                    if ($('.tag-desc li').length < 2) {
                        $('.step-btn .green, .step-btn .red').prop('disabled', false);
                    }
                    break;
                case 3:
                    $('.guide-tab > div:eq(2)').show();
                    $('.intro dt:eq(2)').addClass('current');
                    $('.step-btn .red').show();
                    break;
                case 4:
                    $('.guide-tab > div:eq(3)').show();
                    $('.intro dt:eq(3)').addClass('current');

                     $('.step-btn .red').show();
                     $('.step-btn .green').text(Label.nextStepLabel);

                     $('.intro > div').hide();
                     $('.intro > dl').show();
                    break;
                case 5:
                    $('.guide-tab > div:eq(4)').show();

                    $('.step-btn .red').show();
                    $('.step-btn .green').text(Label.finshLabel);

                    $('.intro > div').show();
                    $('.intro > dl').hide();
                    break;
                case 6:
                    // finished
                    window.location.href = Label.servePath;
                    break;
                default:
                    break;

            }
        };

        $('.step-btn .green').click(function () {
            if (currentStep > 5) {
                return false;
            }
            currentStep++;
            step();
        });

        $('.step-btn .red').click(function () {
            currentStep--;
            step();
        });

        $('.tag-desc li').click(function () {
            var $it = $(this);
            if ($it.hasClass('current')) {
                Util.unfollow(window, $it.data('id'), 'tag');
                $it.removeClass('current');
            } else {
                Util.follow(window, $it.data('id'), 'tag');
                $it.addClass('current');
            }
        });

        step(currentStep);

        $('.tag-desc').isotope({
            transitionDuration: '1.5s',
            filter: 'li',
            layoutMode: 'fitRows'
        });

        // random select one tag

        var random = parseInt(Math.random() * tagSize);
        $('.tag-desc li:eq(' + random + ')').addClass('current');
        Util.follow(window, $('.tag-desc li:eq(' + random + ')').data('id'), 'tag');

    },
    
    /**
     * 职位信息框初始化
     */
    initJob: function () {
    	$.widget( "custom.catcomplete", $.ui.autocomplete, {
    		_renderMenu: function( ul, items ) {
      			var that = this,
        		currentCategory = "";
      			$.each( items, function( index, item ) {
        			if ( item.category != currentCategory ) {
          				ul.append( "<li class='ui-autocomplete-category'>" + item.category + "</li>" );
          				currentCategory = item.category;
        			}
        			that._renderItemData( ul, item );
      			});
    		}
  		});
  		
		$( "#jobDesc" ).catcomplete({
		    delay: 0,
		    source: function( request, response ) {
		      	var requestJSONObject = {
                	keyWords: request.term
            	};
        		$.ajax({
          			url: Label.servePath + "/jobSearch",
          			type: "POST",
          			data: JSON.stringify(requestJSONObject),
          			success: function( data ) {
            			response( $.map( data.jobData, function( item ) {
              				return {
                				code: item.dicCode,
                				label: item.dicValue,
                				category: item.jobCat
              				}
            			}));
              		}
        		});        	
          	},	      
		    minLength: 2,
		    select: function( event, ui ) {
      			$( "#jobId" ).val(ui.item.code);
      		}  
		});
    }
};