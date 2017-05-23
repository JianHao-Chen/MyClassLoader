---
title: 互不重叠的线段
categories: 算法
---

X轴上有N条线段，每条线段有1个起点S和终点E。最多能够选出多少条互不重叠的线段。
（注：起点或终点重叠，不算重叠）。

例如：[1 5][2 3][3 6]，可以选[2 3][3 6]，这2条线段互不重叠。

<!--more-->

---

```bash
class Line{
    int start;
    int end;
}

public class NotOverlappingLine {

static int N = 3;
    
    public static Line[] input(){
        int[] s = {-1,2,1,3,99};
        int[] f = {0,3,5,6,100};
        
        Line[] L = new Line[N+2];
        for(int i=0;i<=N+1;i++){
            L[i] = new Line();
            L[i].start = s[i];
            L[i].end = f[i];
        }
        return L;
    }
    
    /*  
     *  S(i,j)表示 所有与 ai,aj兼容的活动的集合。  
     *          S(i,j) = {ak in S: fi <= sk < fk <= sj}
     *          
     *  即所有属于S(i,j)的活动，与 ai,aj兼容，并且与所有早于ai结束,迟于aj开始的活动兼容。
     *  加上假设的两个活动 a0 , an+1
     *  又已经对活动按结束时间排序，即：
     *      f0 <= f1 <= f2 <= .....<= fn < fn+1
     *      
     *  (1)
     *   当i>=j时， S(i,j)比为空集。
     *      因为：
     *      假设存在ak 属于S(i,j), 有  fi <= sk < fk <= sj < fj
     * 
     *      又i>=j，则有，已排序的 ai在aj后面； fi >=fj
     *      所以矛盾！       
     *  
     *  (2)
     *  问题的子结构：
     *      设S(i,j)的解包含某活动 ak, fi<=sk<fk<=fj。
     *      即可以分为两个子问题： S(i,k) ,S(k,j)
     *      =〉 S(i,j) = S(i,k) + S(k,j) +1
     *      
     *  (3)
     *  设 f[i,j]表示 S(i,j)中最大兼容子集中的活动数。
     *  
     *  i>=j f[i,j] = 0;
     *  f[i,j] = max { f[i,k] + f[k,j] +1 | k=i+1,...j-1 }
     */          
    
    public static void dp(int[][] f,Line[] L){
        int tmp = 0;
        int a,b,c;
        int j;
        for(int l=2;l<=N+1;l++){
            for(int i=0;i<=N-l+1;i++){
                j = i+l;
                if(L[i].end > L[j].start)
                    continue;
                for(int k=i+1;k<=j-1;k++){
                    if(L[k].start >=L[i].end && L[k].end<=L[j].start){
                        a = f[i][k];
                        b = f[k][j];
                        c = f[i][j];
                        if(c<a+b+1){
                            f[i][j] = a+b+1;
                        }
                    }
                }
            }
        }
        
    }
    
    public static void main(String[] args) {
        Line[] L = input();
        int[][] f = new int[N+2][N+2];
        dp(f,L);
        for(int i=0;i<=N+1;i++){
            for(int j=1;j<=N+1;j++){
                System.out.print(f[i][j]+" ");
            }
            System.out.println();
        }
    }
}
```