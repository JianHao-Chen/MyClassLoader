---
title: 编辑距离
categories: 算法
---

编辑距离，又称Levenshtein距离（也叫做Edit Distance），是指两个字串之间，由一个转成另一个所需的最少编辑操作次数。

许可的编辑操作包括将一个字符替换成另一个字符，插入一个字符，删除一个字符。
例如将“kitten”一字转成“sitting”
sitten （k->s）
sittin （e->i）
sitting （->g）
所以kitten和sitting的编辑距离是3。

现在给出两个字符串a,b，求a和b的编辑距离。

<!--more-->

---

```bash
public class EditDistance {
    static char[] X = {'0','K','i','t','t','e','n'};
    static char[] Y = {'0','S','i','t','t','i','n','g'};
    static int lenX = 6;
    static int lenY = 7;
    
    
    /*  
     *  f[i,j] 表示 X[1...i]到 Y[1...j] 的编辑距离。
     *      如果 i==0         ->  f[i,j] = j
     *      如果 j==0         ->  f[i,j] = i
     *      
     *      如果 i,j != 0{
     *          if(X[i]==Y[j])      f[i,j] = f[i-1,j-1]
     *          else
     *              f[i,j] = min { f[i-1,j], f[i,j-1] ,f[i-1,j-1] } +1
     *      }
     */
    public static void dp(int[][]f,int[][]path){
        
        for(int i=1;i<=lenX;i++)
            f[i][0] = i;
        for(int j=1;j<=lenY;j++)
            f[0][j] = j;
        
        int a,b,c,d;
        for(int i=1;i<=lenX;i++){
            for(int j=1;j<=lenY;j++){
                a = f[i-1][j-1];
                if(X[i]==Y[j])
                    f[i][j] = f[i-1][j-1];
                else{
                    b = f[i-1][j];
                    c = f[i][j-1];
                    d = f[i-1][j-1];
                    f[i][j] = min(b,c,d)+1;
                    
                    if(f[i][j] == b+1)
                        path[i][j] = -1;
                    if(f[i][j] == c+1)
                        path[i][j] = 1;
                    if(f[i][j] == d+1)
                        path[i][j] = 2;
                }
                
            }
        }
    }
    
    public static void printPath(int[][] path,int i,int j){
        if(i<1 || j<1) return;
        
        int p = path[i][j];
        
        if(p==0){
            printPath(path,i-1,j-1);
            System.out.println(X[i]+"=="+Y[j]);
        }
        else if(p==1){
            printPath(path,i,j-1);
            System.out.println("behaind "+X[i] +" insert "+ Y[j]);
        }
        else if(p==-1){
            printPath(path,i-1,j);
            System.out.println(" delete" + X[i]);
        }
        else if(p==2){
            printPath(path,i-1,j-1);
            System.out.println(X[i]+" change to "+Y[j]);
        }
    }
    
    public static int min(int a,int b,int c){
        if(a<b){
            if(a<c)
                return a;
            else
                return c;
        }
        else{   //b<=a
            if(b<c)
                return b;
            else
                return c;
        }
    }
    
    public static void main(String[] args) {
        int[][] f = new int[lenX+1][lenY+1];
        int[][] path = new int[lenX+1][lenY+1];
        
        dp(f,path);
        
        for(int i=1;i<=lenX;i++){
            for(int j=1;j<=lenY;j++){
                System.out.print(path[i][j]+" ");
            }
            System.out.println();
        }
        printPath(path,6,7);
    }
}
```