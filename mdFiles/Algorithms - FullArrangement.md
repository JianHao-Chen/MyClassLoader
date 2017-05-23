---
title: 生成全排列
categories: 算法
---

对于给定的集合A{a1,a2,...,an},其中的n个元素互不相同，输出这n个元素的所有排列（全排列）。 
<!--more-->

---

```bash
public class FullArrangement {
    static int N = 3;
    
    /*
     *  全排列array数组的第p个至第q个元素。
     */
    public static void fullArr(char[] array,int p,int q){
        if(p>=q){
            printArray(array);
            return;
        }
        for(int i=p;i<=q;i++){
            swap(array,p,i);
            fullArr(array,p+1,q);
            swap(array,p,i);
        }
    }
    
    public static void printArray(char[] array){
        for(int i=1;i<=N;i++){
            System.out.print(array[i]+" ");
        }
        System.out.println("");
    }
    
    public static void swap(char[] array,int a,int b){
        char tmp = array[a];
        array[a] = array[b];
        array[b] = tmp;
    }
    
    public static void main(String[] args){
        char[] array = {'-','a','b','c'};
        fullArr(array,1,N);
    }
}
```