---
title: 完全背包问题
categories: 算法
---

本文是我学习完全背包问题的笔记。

<!--more-->

---

```bash
/**  
 * 有N种物品和一个容量为V的背包，每种物品都有无限件可用。第i种物品的体积是w[i]，价值是p[i]。
 *  求解将哪些物品装入背包可使这些物品的费用总和不超过背包容量，且价值总和最大。 
 *  
 *  Input :
 *  Knapsack Max weight : W = 10 (units) 
    Total items         : N = 4
    Values of items     : v[] = {10, 40, 30, 50} 
    Weight of items     : w[] = {5, 4, 6, 3}
 *
 *  OutPut :
 *  最大重量为10时背包能容纳的物品最大价值为50+40=90，重量为7
 */

/*  这个问题非常类似于01背包问题，所不同的是每种物品有无限件。也就是从每种物品的角度考虑，
 *  与它相关的策略已并非取或不取两种，而是有取0件、取1件、取2件……直至取[V/w[i]]件等许多种。
    
 *      f[i][j] = max{ f[i-1][j-k*w[i]]+ k*p[i] | k=0,1,2,...,W/w[i] }
 *      共有 O(NW)个状态需要求出。
 *      但是求每一个状态 f[i][j] 需要的时间不是 O(1),而是 O(W/w[i])
 *      
 * 思路一： 转化为01背包问题求解 
 *      考虑到第i种物品最多选V/c [i]件，于是可以把第i种物品转化为
 *      V/c[i]件费用及价值均不变的物品，然后求解这个01背包问题。
 * 
 * 这样完全没有改进基本思路的时间复杂度，但这毕竟给了我们将完全背包问题转化为01背包问题的思路：
 *      将一种物品拆成多件物品。
 *------------------------------------------------------------------------ 
 *------------------------------------------------------------------------
 * 思路二：
 *      把第i种物品拆成费用为w[i]*2^k、价值为p[i]*2^k的若干件物品，
 *      其中k满足w[i]*2^k<V。
 * 
 * 这是二进制的思想，因为不管最优策略选几件第i种物品，总可以表示成若干个2^k件物品的和。
 * 这样把每种物品拆成O(log(V/c[i]))件物品，是一个很大的改进
 */ 
 

public class CompletelyPackage {

    static int N = 0;
    static int W = 0;
    
    
    /*
     * 思路三:
     *       for i=1..N
     *          for v=0..V
     *              f[v]=max{f[v],f[v-cost]+weight}
     *      
     *  这个代码01背包的代码只有v的循环次序不同而已。为什么这样一改就可行呢？
     *  01背包的代码按照v=V..0的逆序来循环。这是因为要保证第i次循环中的状态f[i][v]是由状态
     *  f[i-1][v-c[i]]递推而来。换句话说，这正是为了保证每件物品只选一次，保证在考虑“选入第i件物品”
     *  这件策略时，依据的是一个绝无已经选入第i件物品的子结果f[i-1][v-c[i]]。
     *  
     *  而现在完全背包的特点恰是每种物品可选无限件，所以在考虑“加选一件第i种物品”这种策略时，
     *  却正需要一个可能已选入第i种物品的子结果f[i][v-c[i]]，所以就可以并且必须采用v=0..V的顺序循环。
     *      
     */
    public static void getMaxValue(int[]w,int[]p,int[]f,int[][]path){
        f[0] = 0;
        int a,b;
        
        for(int i=1;i<=N;i++){
            for(int j=1;j<=W;j++){
                a = f[j];
                if(j>=w[i])
                    b = f[j-w[i]]+p[i];
                else
                    b = -1;
                if(b>a)
                    path[i][j] = 1;
                f[j] = a>b?a:b;
            }
        }
    }
    
    
    public static void printPath(int[]w,int[]p,int[][]path){
        int i=N,j=W;
        
        while(i>=1 && j>=1){
            if(path[i][j]==1){
                System.out.print(i+" ");
                j = j-w[i];
            }
            else
                i = i-1;
        }
        System.out.println();
    }
    
    
    public static void main(String[] args) {
        N = 4;       //N为物品的数量
        W = 10;       //W为背包的容量
        
        int[] w = {-1,5,4,6,3}; //物品的体积
        int[] p = {-1,10, 40, 30, 50}; //物品的价值
        
        int[] f = new int[W+1];
        int[][] path = new int[N+1][W+1];
        
        getMaxValue(w,p,f,path);
        
        for(int i=1;i<=N;i++){
            for(int j=1;j<=W;j++){
                System.out.print(path[i][j]+" ");
            }
            System.out.println();
        }
        
        printPath(w,p,path);
    }
}
```