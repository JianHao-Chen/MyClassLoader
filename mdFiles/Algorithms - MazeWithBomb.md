---
title: 逃离迷宫2（深度搜索）
categories: 算法
---

又是迷宫问题，只是这次使用到**剪枝**，所以特别记录下来。


<!--more-->

---
>
在n×m的地图上，0表示墙，1表示空地，2表示人，3表示目的地，4表示有定时炸弹重启器。
定时炸弹的时间是6，人走一步所需要的时间是1。每次可以上、下、左、右移动一格。当人走到4时如果炸弹的时间不是0，可以重新设定炸弹的时间为6。如果人走到3而炸弹的时间不为0时，成功走出。求人从2走到3的最短时间,不能成功走出输出-1.
 
Input
3 3
2 1 1
1 1 0
1 1 3
Output
4

Input
4 8
2 1 1 0 1 1 1 0
1 0 4 1 1 0 4 1
1 0 0 0 0 0 0 1
1 1 1 4 1 1 1 3
Output
-1

Input
5 8
1 2 1 1 1 1 1 4 
1 0 0 0 1 0 0 1 
1 4 1 0 1 1 0 1 
1 0 0 0 0 3 0 1 
1 1 4 1 1 1 1 1
Output
13


【思路】
这个题中每个结点都是可以重复访问的，但其实，炸弹重置点不要重复走，因为，走到炸弹重置点时时间就会被设置为最大时间，当重新返回时时间又设成最大，但此时已走的步数肯定增加了，所以如果存在较优解的话那么肯定在第一次到这点后就可以找到较优解，这也是代码中剪枝的原理，只是将这种思想扩展到普通点而已，所以采用记忆化搜。


```bash
public class Maze {
    static int N = 0;   // 迷宫的行数
    static int M = 0;  // 迷宫的列数
    
    static int[] dx = {0,1,0,-1};
    static int[] dy = {1,0,-1,0};
    
    static int X = 0;
    static int Y = 0;
    
    static int[][] steps;
    static int[][] times;
    
    static int minSteps = 0;
    
    public static void DFS(int[][] maze,int x,int y,int step,int time){
        if(x<=0 || x>N || y<=0 || y>M) return;
        if(time<=0 || step>minSteps) return;
        if(maze[x][y] == 0) return;
        if(maze[x][y] == 3 && step<minSteps){
            minSteps = step;
            return;
        }
        if(maze[x][y]==4){
            time = 6;
        }
        
        // 这里使用了“剪枝”：
        // 与上一次走到这个点的情况比较，
        // 如果当前炸弹的剩余时间比上一次的少，并且已走步数比上一次的多，
        // 那么就不需要在当前情况的基础上继续深搜了。
        if(times[x][y]>=time && steps[x][y]<=step) return;
        steps[x][y] = step;
        times[x][y] = time;
        
        int xx = 0, yy = 0;
        for(int i=0;i<4;i++){
            xx = x+dx[i];
            yy = y+dy[i];
            DFS(maze,xx,yy,step+1,time-1);
        }
        
    }
    
    public static void main(String[] args){
        Scanner sc = new Scanner(System.in);
        N = sc.nextInt();
        M = sc.nextInt();
        
        int x = 0 , y = 0;
        int[][] maze = new int[N+1][M+1];
        steps = new int[N+1][M+1];
        times = new int[N+1][M+1];
        
        for(int i=1;i<=N;i++)
            for(int j=1;j<=M;j++){
                steps[i][j] = Integer.MAX_VALUE;
                times[i][j] = Integer.MAX_VALUE;
                maze[i][j] = sc.nextInt();
                if(maze[i][j]==2){
                    x = i; y = j;
                }
                if(maze[i][j]==3){
                    X = i; Y = j;
                }
            }
        
        minSteps = Integer.MAX_VALUE;
        
        DFS(maze,x,y,0,6);
        
        if(minSteps==Integer.MAX_VALUE)
            System.out.println(-1);
        else
            System.out.println(minSteps);
    }
}
```