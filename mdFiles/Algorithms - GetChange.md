---
title: 找零钱
categories: 算法
---

本文以DP方法解决找零钱问题：
N元钱换为零钱，有多少不同的换法？币值包括1 2 5分，1 2 5角，1 2 5 10 20 50 100元。
例如：5分钱换为零钱，有以下4种换法：
1、5个1分
2、1个2分3个1分
3、2个2分1个1分
4、1个5分

<!--more-->

---

```bash
public class GetChange {

    static int N = 0;
    static int W = 0;
    
    /*  
     *  f[i,j]表示  用前i种币值，余额为j,最多换零钱方法的数量。
     *  
     *  f[i,j] = f[i-1,j] + f[i,j-v[i]]
     *      f[i-1,j]表示不用第i项时,余额为j,换零钱方法数量的最大值。
     *      f[i,j-v[i]]表示用第i项时,余额为j-v[i],换零钱方法数量的最大值。
     */
    public static void getMaxNum(int[] v,int[][]f){
        for(int i=1;i<=W;i++)
            f[1][i] = 1;
        
        for(int i=1;i<=W;i++)
            f[i][0] = 1;
        
        int a,b,c,t;
        for(int i=2;i<=N;i++){
            for(int j=1;j<=W;j++){
                a = f[i-1][j];
                if(j-v[i]>=0){
                    b = j-v[i];
                    c = f[i][b];
                    f[i][j] = a+c;
                }
                else{
                    f[i][j] = a;
                }
            }
        }
    }

    public static void main(String[] args) {
        W = 5;
        
        int[] v = {-1,1,2,5,10,20,50,100,200,500,1000,2000,5000,10000};
        N = v.length-1;
        int[][] f = new int[N+1][W+1];
        
        getMaxNum(v,f);
        
        for(int i=1;i<=N;i++){
            for(int j=1;j<=W;j++){
                System.out.print(f[i][j]+" ");
            }
            System.out.println();
        }
    }
}
```