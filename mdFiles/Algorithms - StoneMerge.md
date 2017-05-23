---
title: 石子合并
categories: 算法
---

N堆石子摆成一条线。现要将石子有次序地合并成一堆。规定每次只能选相邻的2堆石子合并成新的一堆，并将新的一堆石子数记为该次合并的代价。计算将N堆石子合并成一堆的最小代价。

例如： 1 2 3 4，有不少合并方法
1 2 3 4 => 3 3 4(3) => 6 4(9) => 10(19)
1 2 3 4 => 1 5 4(5) => 1 9(14) => 10(24)
1 2 3 4 => 1 2 7(7) => 3 7(10) => 10(20)

括号里面为总代价可以看出，第一种方法的代价最低，现在给出n堆石子的数量，计算最小合并代价。
 
<!--more-->

---

```bash
public class StoneMerge {

    static int N = 0;
    
    /*
     *  V[i,j]表示第i到j个数的合并代价
     *  S[i,j]表示第i到j个数的和

        V[i,j] = min{V[i,r]+V[r+1,j]+S[i..j] | r=i...j}
   */
    public static void Merge(int[][] V,int[][] S,int[] A){
        
        for(int i=1;i<=N;i++)
            S[i][i] = A[i];
        
        for(int i=1;i<=N;i++){
            for(int j=0;j<=N;j++){
                if(i>=j)
                    V[i][j] = 0;
                
                else
                    V[i][j] = 1000;
            }               
        }
        
        int j,t,a,b,c,d;
        for(int l=1;l<=N;l++){
            for(int i=1;i<=N-l+1;i++){
                j = i+l-1;
                S[i][j] = S[i][j-1]+A[j];
                a = S[i][j];
                for(int r=i;r<j;r++){
                    b = V[i][r];
                    c = V[r+1][j];
                    t = V[i][r]+V[r+1][j]+S[i][j];
                    if(t<V[i][j]){
                        V[i][j] = t;
                    }
                }
            }
        }
        
    }
    
    public static void main(String[] args) {
        
        int[] A = {-1,1,2,3,4};
        N = A.length-1;
        
        int[][] V = new int[N+2][N+1];
        int[][] S = new int[N+2][N+1];
        
        Merge(V,S,A);
        
        for(int i=1;i<=N;i++){
            for(int j=1;j<=N;j++){
                System.out.print(V[i][j]+" ");
            }
            System.out.println();
        }
    }
}
```