---
title: 逆序数为K的排列的数目
categories: 算法
---

在一个排列中，如果一对数的前后位置与大小顺序相反，即前面的数大于后面的数，那么它们就称为一个**逆序**。

一个排列中逆序的总数就称为这个排列的**逆序数**。 

如2 4 3 1中,(2 1),(4 3),(4 1),(3 1)是逆序,逆序数是4。
1~n的全排列中，逆序数最小为0（正序），最大为n*(n-1) / 2（倒序）。
给出2个数n和k，求1-n的全排列中，逆序数为k的排列有多少种？
例如：n = 4 k = 3。

1 2 3 4 的排列中逆序为3的共有6个，分别是：

1 4 3 2
2 3 4 1
2 4 1 3
3 1 4 2
3 2 1 4
4 1 2 3
<!--more-->

---

```bash
public class ReverseOrder {
    static int N = 0;
    static int k = 0;
    
    /*
     *  V[n,k]表示n个数的排列中逆序数个数为k的排列数, k=0,...(n*(n-1))/2
     *  因为n可以放在第（1，2，...n）的位置，相应地产生了n-1,n-2,...,0个逆序数
     *  去掉最大数n(放在第n-i | i=0,1,....,n-1的位置)，
     *  则剩下的n-1个数的逆序数个数为k-i;
     *  
     *  所以: V[n,k]   = Sum{V[n-1,k-i] | i=0,1,2,...,n-1}
     *  同理, V[n,k-1] = Sum{V[n-1,k-1-i] | i=0,1,2,...,n-1}
     *  
     *  V[n,k] - V[n,k-1] = V[n-1,k]-V[n-1,k-n]
     *  
     *  => 递推公式:
            V[n,k] = V[n,k-1] + V[n-1,k] - V[n-1,k-n]
     *  
     */
    public static void ReverseOrderNum(int[][] V){
        int nk = 0;
        
        for(int i=1;i<=N;i++)
            V[i][0] = 1;
        
        int a,b,c;
        for(int i=1;i<=N;i++){
            nk = (i*(i-1))/2;
            for(int j=1;j<=nk;j++){
                a = V[i][j-1];
                b = V[i-1][j];
                if(j>=i){
                    c = V[i-1][j-i];
                    V[i][j] = V[i][j-1]+V[i-1][j]-V[i-1][j-i];
                }
                else
                    V[i][j] = V[i][j-1]+V[i-1][j];
            }
            
        }
        
    }
    public static void main(String[] args) {
        N = 4;
        k = 3;
        int[][] V = new int[N+1][N*(N-1)/2+1];
        
        ReverseOrderNum(V);
        
        for(int i=1;i<=N;i++){
            for(int j=0;j<=N*(N-1)/2;j++){
                System.out.print(V[i][j]+" ");
            }
            System.out.println();
        }
    }
}
```