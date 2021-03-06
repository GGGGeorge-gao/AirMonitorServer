package cn.cygao.ams.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * jwt配置文件类
 *
 * @author STEA_YY
 **/
@ConfigurationProperties("jwt-config")
@Data
public class JwtConfig {
    private String secret;
    private int expireTime;
    private int refreshTokenExpireTime;
}
