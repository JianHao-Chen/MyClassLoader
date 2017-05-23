---
title: 插入排序
categories: 算法
---

本文展示插入排序的代码。
<!--more-->

---

```bash
/**
 * 	描述:
 * 		把n个数的一个序列 <a1,a2,a3,...an>,变成
 * 		另一个序列 <b1,b2,b3,...,bn>,并且 b1<=b2<=b3<=...bn.
 * 	思路:
 * 		设  j( 1<=j<=n)是A[1..n]的下标, 
 * 			A[1..j-1] 是已排好序的子序列
 * 			A[j] 是当前正要插入的已排序子序列的元素。
 * 			A[j+1..n] 是未排序的元素。
 *  例子:
 *  	<5,2,4,6,1,3>的插入排序过程:
 *  	a. 5  2  4  6  1  3 	j=2
 *  	b. 2  5  4  6  1  3		j=3
 *  	c. 2  4  5  6  1  3		j=4
 *  	d. 2  4  5  6  1  3		j=5
 *  	e. 1  2  4  5  6  3		j=6
 *  	f. 1  2  3  4  5  6 	
 *
 */

public class InsertionSort {
	
	/*
	 * 排序 A[1...n]共 n个元素。
	 */
	public static void InsertionSort(int[] A,int n){
		int key = 0;
		int i = 0;
		for(int j=2;j<=n;j++){
			key = A[j];
			
			//将 A[j]与 A[1..j-1]比较(并且插入到合适位置)
			i = j-1;
			while(key<A[i] && i>=1){
				A[i+1] = A[i];
				i--;
			}
			A[i+1] = key;
			SortingUtil.printArray(A, n);
		}
	}
	
	public static void main(String args[]){
		int[] A = {-1,5,2,4,6,1,3};
		InsertionSort(A,A.length-1);
	}
}
```