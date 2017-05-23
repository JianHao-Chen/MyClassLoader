---
title: 逃离迷宫（深度搜索）
categories: 算法
---

给定一个m × n (m行, n列)的迷宫，迷宫中有两个位置，gloria想从迷宫的一个位置走到另外一个位置，当然迷宫中有些地方是空地，gloria可以穿越，有些地方是障碍，她必须绕行，从迷宫的一个位置，只能走到与它相邻的4个位置中,当然在行走过程中，gloria不能走到迷宫外面去。令人头痛的是，gloria是个没什么方向感的人，因此，她在行走过程中，不能转太多弯了，否则她会晕倒的。
我们假定给定的两个位置都是空地，初始时，gloria所面向的方向未定，她可以选择4个方向的任何一个出发，而不算成一次转弯。gloria能从一个位置走到另外一个位置吗？

Input
第1行为两个整数m, n (1 ≤ m, n ≤ 100),分别表示迷宫的行数和列数，
接下来m行，每行包括n个字符，
其中字符'.'表示该位置为空地，字符'*'表示该位置为障碍，输入数据中只有这两种字符，
最后一行为5个整数
k (1 ≤ k ≤ 10), k表示gloria最多能转的弯数
x1, y1, x2, y2 (, 1 ≤ x 1, x 2 ≤ n, 1 ≤ y 1, y 2 ≤ m),
(x 1, y 1), (x 2, y 2)表示两个位置，其中x 1，x 2对应列，y 1, y2对应行。

Sample Input
5 5
...**
*.**.
.....
.....
*....
1 1 1 3 1
Sample Output
no

---

Sample Input
5 5
...**
*.**.
.....
.....
*....
2 1 1 3 1 
Sample Output
yes


<!--more-->

---

```bash
public class FleeTheMaze {

    static int M = 5;   // 迷宫的行数
    static int N = 5;  // 迷宫的列数
    static int[] dx = {-1,0,1,0};
    static int[] dy = {0,1,0,-1};
    static int[][] turned = new int[M+1][N+1];  //记下转弯次数.
    static char[][] maze = new char[M+1][N+1];
    
    static int k,x1,y1,x2,y2;
    static int ok = 0;  //用于标志查找结果 , ok==1表示成功.
    
    public static void DFS(int x, int y ,int dir){
        int i,xx,yy;
        
        // 找到并且转弯次数<=k
        if(x==x2 && y==y2 && turned[x][y]<=k){
            ok = 1;
            return;
        }
        if(turned[x][y]>k)         //大于k时不满足条件，返回  
            return ;
        //如果(x,y)和终点(x2,y2),既不在同一行也不在同一列，
        //那么要想到终点至少需要转一次，但现在已经转够k次了，故不行
        if(x!=x2 && y!=y2 && turned[x][y]==k)
            return ;
        
        for(i=0;i<4;i++){
            xx = x+dx[i];
            yy = y+dy[i];
            if(xx<=0 || yy<=0 || xx>M || yy>N || maze[xx][yy]=='*')  
                continue;
            
            /*
             * 注意！！！
             * 下面这行中的turn[xx][yy]是表示(xx,yy)已经由别的路线走过了，
             * 并记录了turn[xx][yy]，现在需要比较这次走到(xx,yy)和由别的路线走到  
               (xx,yy)时，两个的turn值，如果上次的比这次的小，说明这次不行，故要continue；
             */
            if(turned[xx][yy]<turned[x][y])      //这里相等的情况不能剪掉，原因开头已解释     
                continue;
            /*
             *  目的是：如果从(x,y)走一步到(xx,yy)需要转一次话，并且转过之后
             *  turn[x][y]+1依然比turn[xx][yy]大的话，也不符合 
             */
            if(dir!=-1 && i!=dir && turned[xx][yy]<turned[x][y]+1)  
                continue;           //这两个if语句剪枝很重要，没有的话就超时  
            
            
            //如果方向变了转弯+1
            if(dir!=-1 && i!=dir)  
                turned[xx][yy]=turned[x][y]+1;  
            else  
                turned[xx][yy]=turned[x][y];
            
            //如果这里能走，就把这里变成不能走，然后再从这里开始递归，
            //其实就是起到vis[][] 的作用
            maze[xx][yy]='*';    
            
            DFS(xx,yy,i);
            maze[xx][yy]='.';
            
            if(ok==1)  
                return ;  
        }
    }
    
    public static void main(String[] args){
        
        Scanner sc = new Scanner(System.in);
        
        for(int i=1;i<=M;i++){
            String s = sc.next();
            for(int j=1;j<=N;j++){
                char c = s.charAt(j-1);
                maze[i][j] = c;
            }
        }
        
        for(int i=1;i<=M;i++){
            for(int j=1;j<=N;j++){
                System.out.print(maze[i][j]+" ");
            }
            System.out.println("");
        }
        
        k = sc.nextInt();
        x1 = sc.nextInt();
        y1 = sc.nextInt();
        x2 = sc.nextInt();
        y2 = sc.nextInt();
        
        // 初始化转弯次数
        for(int i=1;i<=M;i++){
            for(int j=1;j<=N;j++){
                turned[i][j] = Integer.MAX_VALUE;
            }
        }
        
        turned[x1][y1] = 0;
        DFS(x1,y1,-1);
        if(ok==1)
            System.out.println("Yes");
        else
            System.out.println("NO");
        
    }
}
```