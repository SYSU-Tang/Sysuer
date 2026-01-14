package com.sysu.edu.life;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.sysu.edu.R;
import com.sysu.edu.databinding.ActivityGymPreservationBinding;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Objects;

public class GymReservationActivity extends AppCompatActivity {
    /**
     * 计算字符串的SHA1哈希值，返回十六进制字符串
     */
    public static String hexSha1(String input) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("SHA-1");
        byte[] hash = md.digest(input.getBytes());
        return bytesToHex(hash);
    }

    /**
     * 将字节数组转换为十六进制字符串
     */
    private static String bytesToHex(byte[] bytes) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : bytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }

    /**
     * 将十六进制字符串转换为二进制字符串 每个十六进制字符转换为4位二进制
     */
    public static String hexToBinary(String hexStr) {
        StringBuilder binaryStr = new StringBuilder();
        for (int i = 0; i < hexStr.length(); i++) {
            char c = hexStr.charAt(i);
            int value = Character.digit(c, 16);
            // 转换为4位二进制，前面补0
            String binary = String.format("%4s", Integer.toBinaryString(value)).replace(' ', '0');
            binaryStr.append(binary);
        }
        return binaryStr.toString();
    }

    /**
     * 模拟JS中的bin_sha1函数
     */
    public static String binSha1(String input) throws NoSuchAlgorithmException {
        String hexHash = hexSha1(input);
        return hexToBinary(hexHash);
    }

    /**
     * 找到满足条件的suffix
     */
    public static String findSuffix(String prefix, int leadingZeroBit) throws NoSuchAlgorithmException {
        int cnt = 0;
        while (true) {
            String suffix = Integer.toHexString(cnt);
            String hashBinary = binSha1(prefix + suffix);
            // 检查前leadingZeroBit位是否全为0
            if (hashBinary.substring(0, leadingZeroBit).equals("0".repeat(leadingZeroBit))) {
                return suffix;
            }
            cnt++;
        }
    }

    /**
     * 计算最终的safeline_bot_challenge_ans cookie值
     */
    public static String getFinalCookie(String safelineBotChallenge, String prefix, int leadingZeroBit)
            throws NoSuchAlgorithmException {
        return safelineBotChallenge + findSuffix(prefix, leadingZeroBit);
    }

    public static String encode(String prefix, String safelineBotChallenge) {
        try {
            return getFinalCookie(safelineBotChallenge, prefix, 9);
        } catch (NoSuchAlgorithmException e) {
            System.err.println("SHA-1算法不可用: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("发生错误: " + e.getMessage());
        }
        return "";
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityGymPreservationBinding binding = ActivityGymPreservationBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        NavHostFragment fragment = (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.content);
        NavController navController = Objects.requireNonNull(fragment).getNavController();
        NavigationUI.setupWithNavController(binding.toolbar, navController, new AppBarConfiguration.Builder().setFallbackOnNavigateUpListener(() -> {
            supportFinishAfterTransition();
            return true;
        }).build());
        NavigationUI.setupWithNavController(binding.bottomNavigation, navController);

    }
}