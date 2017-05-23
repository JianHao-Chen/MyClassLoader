---
title: 和相差最小的数集
categories: 算法
---

将一堆正整数分为2组，要求2组的和相差最小。
例如：1 2 3 4 5，将1 2 4分为1组，3 5分为1组，两组和相差1，是所有方案中相差最少的。

<!--more-->

---

```bash
public class NumSet {

    static int N = 0;
    static int Sum = 0;
    static int Rest = 0;
    
    /*
     * 先算出数组的平均值,然后这个平均值就相当于背包的容量,求把这个“物品”放到这个背包的
     * 最大值。
     */
    public static void cout(int[][] V,int[] A,int[][]Path){
        
        for(int i=1;i<=N;i++){
            V[i][0] = 0;
        }
        for(int i=1;i<=Sum;i++){
            V[0][i] = 0;
        }
        
        int a,b,c,max;
        
        for(int i=1;i<=N;i++){
            for(int j=1;j<=Sum/2;j++){
                a = V[i-1][j];
                if(A[i]<=j){
                    b = V[i-1][j-A[i]];
                    c = b+A[i];
                    if(c>a)
                        Path[i][j] = 1;
                    
                    max = a>c?a:c;
                    if(max>V[i][j])
                        V[i][j] = max;
                }
            }
        }
    }
    
    public static void main(String[] args) {
        
        N = 5;
        int[] A = {-1,1,2,3,4,5};
        
        for(int i=1;i<=N;i++){
            Sum +=A[i];
        }
        
        
        int[][] V = new int[N+1][Sum+1];
        int[][] path = new int[N+1][Sum+1];
        
        cout(V,A,path);
        
        for(int i=1;i<=N;i++){
            for(int j=1;j<=Sum;j++){
                //if(Path[i][j]==1)
                    System.out.print(V[i][j]+" ");
            }
            System.out.println();
        }
    }
}
```