---
title: 01背包问题
categories: 算法
---

本文是我学习01背包问题的笔记。
<!--more-->

---

```bash
/**  
 *  有N件物品和一个容量为W的背包。第i件物品的体积是w[i]，价值是p[i]。
 *  求解将哪些物品装入背包可使这些物品的费用总和不超过背包容量，且价值总和最大。
 * 
 *  每种物品仅有一件，可以选择放或不放。
 *  
 *  Input :
 *  Knapsack Max weight : W = 10 (units) 
    Total items         : N = 4
    Values of items     : v[] = {10, 40, 30, 50} 
    Weight of items     : w[] = {5, 4, 6, 3}
 *
 *  OutPut :
 *  最大重量为10时背包能容纳的物品最大价值为50+40=90，重量为7
 *
 */

public class _01_Package {

    static int N = 0;
    static int W = 0;
    
    
    /*
     *  f[i][v]表示前i件物品恰放入一个容量为v的背包可以获得的最大价值.
     *  
     *  状态转移方程:
     *  f[i][v] = max{f[i-1][v],f[i-1][v-w[i]]+p[i]}
     *  
     *  解释:
     *  “将前i件物品放入容量为v的背包中” 这个子问题，
     *  若只考虑第i件物品的策略（放或不放），那么就可以转化为一个只牵涉前i-1件物品的问题。
     * 
     *  如果  不放第i件物品，那么问题就转化为“前i-1件物品放入容量为v的背包中”；
     *  如果   放第i件物品，   那么问题就转化为“前i-1件物品放入剩下的容量为v-c[i]的背包中”，
     *  
     *  此时能获得的最大价值就是f[i-1][v-c[i]]再加上通过放入第i件物品获得的价值w[i]。
     *  
--------------------------------------------------------------------------------------
--------------------------------------------------------------------------------------
     *  初始化的F数组事实上就是在没有任何物品可以放入背包时的合法状态。
     *  
     *  如果是 : 要求恰好装满背包，
     *          那么在初始化时除了F[0]为0，其它F[1..V]均设为 -∞，
     *          这样就可以保证最终得到的F[V]是一种恰好装满背包的最优解。
     *          
     *     只有容量为0的背包可以在什么也不装且价值为0的情况下被“恰好装满”，
     *     其它容量的背包均没有合法的解，属于未定义的状态，应该被赋值为-∞了
     *     
     *     
     *  如果是 : 并没有要求必须把背包装满，而是只希望价格尽量大，
                                    初始化时应该将F[0::V ]全部设为0。
                
                        那么任何容量的背包都有一个合法解“什么都不装”，这个解的价值为0，
                        所以初始时状态的值也就全部为0了。
              
                最终的答案并不一定是f[N][V],而是f[N][0..V]的最大值
     *  
     */
    public static void getMaxValue(int[]w,int[]p,int[][]f,int[][]path){
        
        for(int i=1;i<=N;i++)
            f[i][0] = 0;
        
        int notChoose,choose;
        for(int i=1;i<=N;i++){
            for(int j=1;j<=W;j++){
                notChoose = f[i-1][j];  // 不选第i项物品时的总价值
                if(j>=w[i])             // 第i项物品的重量是否大于当前剩余空间
                    choose = f[i-1][j-w[i]]+p[i];
                else choose = -1;
                
                if(choose>notChoose)
                    path[i][j] = 1;
                f[i][j] = notChoose>choose?notChoose:choose;
            }
        }
    }
    
    
    
    public static void printPath(int[]w,int[]p,int[][]path){
        int i = N, j = W;
        
        while(i>=1 && j>=1){
            if(path[i][j]==1){
                System.out.print(i+" ");
                j = j-w[i];
            }
            i--;
        }
        System.out.println();
    }
    
    
    
    /*
     * 状态方程:
     *      f[j] = max{f[j],f[j-w[i]]+p[i]}
     */
    public static void getMaxValue_CompressionSpace(int[]w,int[]p,int[]g,int[][] path){
        g[0] = 0;
        int a,b;
        for(int i=1;i<=N;i++){
            for(int j=W;j>=1;j--){
                a = g[j];
                if(j>=w[i])
                    b = g[j-w[i]]+p[i];
                else
                    b = -1;
                if(b>a)
                    path[i][j] = 1;
                g[j] = a>b?a:b;
            }
        }
        
    }
    
    public static void main(String[] args) {
        N = 4;       //N为物品的数量
        W = 10;       //W为背包的容量
        
        int[] w = {-1,5,4,6,3}; //物品的体积
        int[] p = {-1,10, 40, 30, 50}; //物品的价值
        
        int[][] path = new int[N+1][W+1];
        
        int[][] f = new int[N+1][W+1];
        
        /*getMaxValue(w,p,f,path);
        printPath(w,p,path);
        
        for(int i=1;i<=N;i++){
            for(int j=1;j<=W;j++){
                System.out.print(f[i][j]+" ");
            }
            System.out.println();
        }*/
        
        
        int[] g = new int[W+1];
        
        getMaxValue_CompressionSpace(w,p,g,path);
        printPath(w,p,path);
        for(int i=1;i<=W;i++)
            System.out.print(g[i]+" ");
        
    }
}
```