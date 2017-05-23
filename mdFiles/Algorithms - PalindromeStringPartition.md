---
title: 回文串划分
categories: 算法
---

有一个字符串S，求S最少可以被划分为多少个回文串。
例如:  abbaabaa, 有多种划分方式。

a|bb|aabaa - 3 个回文串
a|bb|a|aba|a - 5 个回文串
a|b|b|a|a|b|a|a - 8 个回文串

其中第1种划分方式的划分数量最少。
<!--more-->

---

```bash
public class PalindromeStringPartition {

    static int N = 0;
    
    
    /*
     *   flag[i][j]表示长度为i,从第j个字符开始的字符串，是否回文串。
     */
    public static void checkPalindrome(char[] ps,int[][] flag){
        for(int i=1;i<=2;i++){  //初始化长度为2的字符串是否回文串
            for(int j=1;j<=N-i+1;j++){
                if(ps[j]==ps[j+i-1])
                    flag[i][j] = 1;
            }
        }
        
        // 处理长度大于2的字符串
        for(int i=3;i<=N;i++){
            for(int j=1;j<=N-i+1;j++){
                if(flag[i-2][j+1]==1 && ps[j]==ps[j+i-1])
                    flag[i][j] = 1;
            }
        }
        
    }
    
    /*
     * dp[i] 表示从第1个到第i个字符的回文串的最小划分数。
     * 
     * dp[0] = 0 
     * 
     *  dp[i] = min 
     *  {
     *      dp[i-j] (&& s[i-j+1]....s[i]是回文串 )
     *      j=[1,2,...,i]   j为扫描的长度
     *      即从当前i字符开始向前找出最长的回文串，
     * 
     *      dp[i-j]就是  字符s[i-j+1]的前一个的最小划分值
     * 
     *  }
     */
    public static void getMinPartitionNum(char[] ps,int[][] flag,int[]dp,int[]path){
        // 初始化dp[]
        for(int i=1;i<=N;i++){
            dp[i] = 100;
        }
        
        for(int i=1;i<=N;i++){
            // j是扫描的长度
            for(int j=1;j<=i;j++){
                if(flag[j][i-j+1]==1 && dp[i-j]+1<dp[i]){
                    dp[i] = dp[i-j]+1;
                }
            }
        }
    }
    
    public static void main(String[] args) {
        String s = "abbaabaa";
        N = s.length();
        
        char[] ps = new char[s.length()+1];
        for(int i=1;i<=s.length();i++)
            ps[i] = s.charAt(i-1);
        
        int[][] flag = new int[N+1][N+1];
        int[] dp = new int[N+1];
        
        int[] path = new int[N+1];
        
        checkPalindrome(ps,flag);
        
        getMinPartitionNum(ps,flag,dp,path);
        
        System.out.println(dp[N]);
    }
}
```