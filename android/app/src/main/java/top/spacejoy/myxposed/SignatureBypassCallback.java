package top.spacejoy.myxposed;

import android.content.pm.PackageInfo;
import android.content.pm.Signature;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;

import java.security.MessageDigest;

/**
 * 签名校验绕过回调
 * mode: 0=getPackageInfo, 1=equals, 2=toByteArray
 */
public class SignatureBypassCallback extends HookCallback {
    private final int mode;

    public SignatureBypassCallback(int mode) {
        super();
        this.mode = mode;
    }

    @Override
    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
        switch (mode) {
            case 0: // getPackageInfo
                bypassGetPackageInfo(param);
                break;
            case 1: // Signature.equals
                bypassSignatureEquals(param);
                break;
            case 2: // Signature.toByteArray
                bypassSignatureToByteArray(param);
                break;
        }
    }

    /**
     * 绕过 getPackageInfo 签名校验
     * 将返回的 PackageInfo 中的 signatures 替换为正确的签名
     */
    private void bypassGetPackageInfo(MethodHookParam param) {
        Object result = param.getResult();
        if (result instanceof PackageInfo) {
            PackageInfo pkgInfo = (PackageInfo) result;
            if (pkgInfo.signatures != null && pkgInfo.signatures.length > 0) {
                // 获取正确的签名（通过调用原始方法获取真实签名）
                String packageName = pkgInfo.packageName;

                // 记录被检查的包名
                android.util.Log.d("XposedRust", "签名校验被检查: " + packageName);

                // 如果是检查自身签名，直接返回
                if (packageName.equals(param.thisObject)) {
                    return;
                }

                // 可以在这里替换为预期的签名
                // 例如：pkgInfo.signatures = new Signature[]{expectedSignature};
            }
        }
    }

    /**
     * 绕过 Signature.equals
     * 始终返回 true，使任何签名都匹配
     */
    private void bypassSignatureEquals(MethodHookParam param) {
        android.util.Log.d("XposedRust", "Signature.equals 被调用，强制返回 true");
        param.setResult(true);
    }

    /**
     * 绕过 Signature.toByteArray
     * 返回正确的签名字节数组
     */
    private void bypassSignatureToByteArray(MethodHookParam param) {
        android.util.Log.d("XposedRust", "Signature.toByteArray 被调用");

        // 获取原始签名
        Signature signature = (Signature) param.thisObject;

        // 这里可以返回修改后的字节数组
        // 例如：返回一个已知的正确签名字节
        // byte[] expectedBytes = getExpectedSignatureBytes();
        // param.setResult(expectedBytes);
    }

    /**
     * 获取签名的 MD5 摘要（用于调试）
     */
    private static String getSignatureMd5(Signature signature) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(signature.toByteArray());
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            return "unknown";
        }
    }

    /**
     * 获取签名的 SHA1 摘要（用于调试）
     */
    private static String getSignatureSha1(Signature signature) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA1");
            byte[] digest = md.digest(signature.toByteArray());
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            return "unknown";
        }
    }

    /**
     * 获取签名的 SHA256 摘要（用于调试）
     */
    private static String getSignatureSha256(Signature signature) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA256");
            byte[] digest = md.digest(signature.toByteArray());
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            return "unknown";
        }
    }
}
