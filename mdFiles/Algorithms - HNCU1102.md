---
title: 迷宫之求最短路程(广度搜索)
categories: 算法
---
小明置身于一个迷宫，请你帮小明找出从起点到终点的最短路程。
小明只能向上下左右四个方向移动。 
        
输入的第一行是两个整数N和M（1<=N,M<=100）。
接下来N行，每行输入M个字符，每个字符表示迷宫中的一个小方格。
字符的含义如下：
‘S’：起点
‘E’：终点
‘-’：空地，可以通过
‘#’：障碍，无法通过
输入数据保证有且仅有一个起点和终点。

输出从起点到终点的最短路程，如果不存在从起点到终点的路，则输出-1

<!--more-->

---

```bash

/**
* 测试数据：
*  Input
 *  5 5
    S-###
    -----
    ##---
    E#---
    ---##
 *    
 *  Output
 *  9
*/

public class HNCU1102 {

    static int N = 0;   // 迷宫的行数
    static int M = 0;  // 迷宫的列数
    static int[] dx = {0,1,0,-1};
    static int[] dy = {1,0,-1,0};
    
    static int Ex = 0;
    static int Ey = 0;
    
    static int[][] visited;
    static int minSteps = Integer.MAX_VALUE;
    
    public static void BFS(char[][] maze,int Sx,int Sy){
        Queue<Point> queue = new LinkedList<Point>();
        Point p = new Point(Sx,Sy,0);
        visited[Sx][Sy] = 1;
        queue.offer(p);
        while(!queue.isEmpty()){
            Point q = queue.poll();
            visited[q.x][q.y] = 1;
            if(q.x==Ex && q.y==Ey){
                if(q.step<minSteps)
                    minSteps = q.step;
                return;
            }
            int xx , yy;
            for(int i=0;i<4;i++){
                xx = q.x+dx[i];
                yy = q.y+dy[i];
                if(xx<=0 || xx>N || yy<=0 || yy>M || maze[xx][yy]=='#')continue;
                if(visited[xx][yy]==1) continue;
                queue.offer(new Point(xx,yy,q.step+1));
                visited[xx][yy] = 1;
            }
        }
        
    }
    
    public static void main(String[] args){
        Scanner sc = new Scanner(System.in);
        N = sc.nextInt();
        M = sc.nextInt();
        int Sx = 0 , Sy = 0;
        visited = new int[N+1][M+1];
        
        char[][] maze = new char[N+1][M+1];
        
        for(int i=1;i<=N;i++){
            String s = sc.next();
            for(int j=1;j<=M;j++){
                char c = s.charAt(j-1);
                maze[i][j] = c;
                if(c=='S'){Sx = i; Sy = j;}
                if(c=='E'){Ex = i; Ey = j;}
            }
        }
        
        BFS(maze,Sx,Sy);
        
        System.out.println(minSteps);
    }
}

class Point{
    int x;
    int y;
    int step;
    
    public Point(){}
    public Point(int x, int y,int step) {
        super();
        this.x = x;
        this.y = y;
        this.step = step;
    }
    
    public String toString(){
        return "["+x+","+y+"]"+"("+step+")";
    }
}
```