---
title: 最大子矩阵和
categories: 算法
---

一个M*N的矩阵，找到此矩阵的一个子矩阵，并且这个子矩阵的元素的和是最大的，输出这个最大的值。
例如：3*3的矩阵：
-1   3  -1
2  -1   3
-3   1   2

和最大的子矩阵是：
3 -1
-1  3
1  2

<!--more-->

---

```bash
public class MaxSubMatrix {
    static int M = 0;
    static int N = 0;

    public static int getMaxSubSum(int[] a){
        int b,max;
        if(a[1]>=0)
            b = a[1];
        else
            b = 0;
        max = b;
        for(int i=2;i<=M;i++){
            if(b<0)
                b = a[i];
            else
                b = b + a[i];
            if(b>max)
                max = b;
        }
        return max;
    }
    
    
    /*
     *  如果用枚举方法,时间复杂度为 : O(M^2 * N^2)
     *  
     *  利用最大子段和的程序,可以 将多行相加作为一行(临时保存),求出这个叠加行的最大子段和,
     *  就是一个子矩阵的最大子段和。
     *  
     *  于是时间复杂度为 : O(N^2 * M)
     *  
     */
    public static int getMaxSubMatrix(int[][]A){
        
        int[] b = new int[M+1];
        int max = 0,tmp;
        
        for(int i=1;i<=N;i++){
            for(int k=1;k<=M;k++)
                b[k] = 0;
            
            for(int j=i;j<=N;j++){
                for(int k=1;k<=M;k++){
                    b[k]+= A[j][k];
                }
                tmp = getMaxSubSum(b);
                if(tmp>max)
                    max = tmp;
            }
        }
        return max;
    }
    
    public static void main(String[] args) {
        
        M = 3;
        N = 3;
        int[][] A = {
                {0,0,0,0},
                {0,-1,3,-1},
                {0,2,-1,3},
                {0,-3,1,2}
        };
        
        for(int i=1;i<=N;i++){
            for(int j=1;j<=M;j++){
                System.out.print(A[i][j]+" ");
            }
            System.out.println();
        }
        
        System.out.println(getMaxSubMatrix(A));
    }
}
```