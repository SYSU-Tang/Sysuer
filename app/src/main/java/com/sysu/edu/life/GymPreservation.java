package com.sysu.edu.life;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import com.sysu.edu.R;
import com.sysu.edu.databinding.ActivityGymPreservationBinding;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class GymPreservation extends AppCompatActivity {

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
        String suffix = findSuffix(prefix, leadingZeroBit);

        // 拼接最终的cookie值

        // System.out.println("-".repeat(50));
        // System.out.println("最终的 safeline_bot_challenge_ans: " + finalCookie);

        return safelineBotChallenge + suffix;
    }

    public static String encode(String prefix, String safelineBotChallenge) {
        try {
            int leadingZeroBit = 9;
            return getFinalCookie(safelineBotChallenge, prefix, leadingZeroBit);
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
        binding.toolbar.setNavigationOnClickListener(v -> supportFinishAfterTransition());
        NavHostFragment fragment = (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.gym_preservation_campus_fragment);
        if (fragment != null) {
            NavController navController = fragment.getNavController();
        }

    }
}

/*
class DateAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    final Context context;

    public DateAdapter(Context context) {
        super();
        this.context = context;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        View item = LayoutInflater.from(context).inflate(R.layout.item_date, parent, false);

        return new RecyclerView.ViewHolder(item) {
        };
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {

        ((MaterialTextView) holder.itemView.findViewById(R.id.date)).setText(getDate(position));
        ((MaterialTextView) holder.itemView.findViewById(R.id.week)).setText(String.format("星期%s", getWeek(position)));
    }

    @Override
    public int getItemCount() {
        return 3;
    }

    String getDate(int distanceDay) {
        Calendar date = Calendar.getInstance();
        date.add(Calendar.DATE, distanceDay);
        return new SimpleDateFormat("MM月dd日", Locale.CHINESE).format(date.getTime());
    }

    String getWeek(int distanceDay) {
        Calendar date = Calendar.getInstance();
        date.add(Calendar.DATE, distanceDay);
        int week = date.get(Calendar.DAY_OF_WEEK);
        week = (week == 1) ? 6 : week - 1;
        return context.getResources().getStringArray(R.array.weeks)[week];
    }
}*/
