---
title: 最大子段和
categories: 算法
---

N个整数组成的序列a[1],a[2],a[3],…,a[n]，
求该序列如a[i]+a[i+1]+…+a[j]的连续子段和的最大值。当所给的整数均为负数时和为0。
例如：-2,11,-4,13,-5,-2，和最大的子段为：11,-4,13。和为20。

<!--more-->

---

```bash
public class MaxSubSeqSum {
    static int N = 0;
    
    
    /*
     * 方法一: 枚举i和j，求i和a[i]到a[j]之间的和的最大值
     * 
     * 时间复杂度O（n^3）
     */
    public static int getMaxSubSeqSum1(int[] A){
        
        int max = 0;
        if(A[1]>=0)
            max = A[1];
        
        for(int i=2;i<=N;i++){
            for(int j=2;j<=i;j++){
                int tmp_sum = 0;
                for(int k=j;k<=i;k++){
                    tmp_sum +=A[k];
                }
                if(tmp_sum>max)
                    max = tmp_sum;
            }
        }
        return max;
    }
    
    /*
     * 方法一中的最后一个循环是重复计算， 可以省略。
     * 时间复杂度变成O（n^2）
     * 
     */
    public static int getMaxSubSeqSum2(int[] A){
        
        int max = 0;
        int sum = 0;
        for(int i=1;i<=N;i++){
            sum = 0;
            for(int j=i;j<=N;j++){
                sum += A[j];
                if(sum>max)
                    max = sum;
            }
        }
        return max;
    }
    
/*
* 分治法求解.
* 
* 将序列a[1:n]分成长度相等的两段a[1:n/2]和a[(n/2)+1:n],分别求出这两段的最大字段和.
* 则a[1:n]的最大子段和有三种情形:
* 
*   1. 与a[1:n/2]的最大子段和相同
*   2. 与a[n/2+1:n]的最大子段和相同
*   3. a[1:n]的最大子段和由 [S1,S2]组成,其中S1属于a[1:n/2], S2属于a[n/2+1:n]
* 
*   时间复杂度 O(nlogn)
*/
    public static int getMaxSubSeqSum3(int[] A , int left , int right){
        int sum = 0;
        if(left == right){   //如果序列长度为1，直接求解
            if(A[left] > 0) 
                sum = A[left];
            else 
                sum = 0;
        }
        else{
            int center = (left + right) / 2;    //划分
            int leftsum = getMaxSubSeqSum3(A,left,center);    //对应情况1，递归求解
            int rightsum = getMaxSubSeqSum3(A, center + 1, right);//对应情况2， 递归求解
            
            
            int s1 = 0; int lefts = 0;
            //求解s1
            for(int i = center; i >= left; i--){
                lefts += A[i];
                if(lefts > s1) 
                    s1 = lefts;  //左边最大值放在s1
            }
            
            int s2 = 0; int rights = 0;
            //求解s2
            for(int j = center + 1; j <= right; j++){
                rights += A[j];
                if(rights > s2)
                    s2 =rights;
            }
            
            //计算第3钟情况的最大子段和
            sum = s1 + s2;
            
            //合并,在sum、leftsum、rightsum中取最大值
            if(sum < leftsum) 
                sum = leftsum;    
            if(sum < rightsum) 
                sum = rightsum;
        }
        
        return sum;
    }
    
    /*
     * 动态规划
     * 
     */
    public static int getMaxSubSeqSum4(int[] A){
        
        int max = -Integer.MAX_VALUE;
        int sum = 0;
        for(int i=1;i<=N;i++){
            if(sum + A[i] < 0)
                sum = 0;
            else
                sum = sum + A[i];
            
            if(sum>max)
                max = sum;
        }
        return max;
    }
    
    
    
    /**
     *  循环数组最大子段和
     *  本题与普通的最大子段和问题不同的是，最大子段和可以是首尾相接的情况，即可以循环。
     *  
     *  这个题目的最大子段和有两种情况 :
     *      （1）正常数组中间的某一段和最大。这个可以通过普通的最大子段和问题求出。
     *      （2）此数组首尾相接的某一段和最大。这种情况是由于数组中间某段和为负值，且绝对值很大导致的， 我们只需要把中间的和为负值且绝对值最大的这一段序列求出，用总的和减去它就行了。
     */
    public static int getMaxSubCircleSeqSum(int[] A){
        int sum = 0;
        for(int i=1;i<=N;i++){
            sum +=A[i];
            A[i] = -A[i];
        }
        
        int max = sum+getMaxSubSeqSum4(A);
        return max;
    }
    
    public static void main(String[] args) {
        /*int[] A = {-1,-2,11,-4,13,-5,-2};
        N = A.length -1;
        System.out.println(getMaxSubSeqSum4(A));*/
        
        //System.out.println(getMaxSubSeqSum3(A,1,N));
        
        int[] A = {-1,1,2,-4,-5,3};
        N = A.length -1;
        System.out.println(getMaxSubCircleSeqSum(A));
        
    }
}
```