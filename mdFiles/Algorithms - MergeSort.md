---
title: 归并排序
categories: 算法
---

本文展示归并排序的代码。
<!--more-->

---

```bash
/**
 * 				******  归并排序    ******
 * 	采用分治法:
 * 	【分解】 分解待排序的n个元素的序列成各具n/2个元素的2个子序列。
 *  【解决】 使用归并排序递归排序2个子序列
 *  【合并】 合并2个已经排序的子序列以产生已排序的序列。
 *  “递归出口”是 待排序序列的长度为1.
 *
 *  合并过程由  Merge(A,p,q,r)完成，其中 A[p,q]和A[q+1,r]都是
 *  已排序的序列，这个方法合并2个子序列为一个排好序的序列。
 *  
 *  【特点】：
 *  	时间复杂度：
 *  		平均情况 : O(n*log n)
 *  		最好情况 : O(n*log n)
 *  		最坏情况 : O(n*log n)
 *  	空间复杂度 ：
 *  		O(n)
 *  	稳定性     : 稳定
 *  
 *  
 */
public class MergeSort {

	private static int N = 0;
	
	/*
	 *  排序 A[p..r]中的元素。
	 *  如果 p>=r , 则表示此子序列最多有一个元素，即已排序。
	 *  否则，计算下标 q, q = (p+r)/2  <向下取整>
	 *  	使得将 A[p..r]分解为A[p..q]和A[q+1..r].
	 *  于是 ， A[p..q]包含 n/2(向上取整) 个元素。
	 *  	 A[q+1..r]包含 n/2(向下取整) 个元素。
	 */
	public static void MergeSort(int[] A,int p,int r){
		if(p<r){
			int q = (p+r)/2;
			MergeSort(A,p,q);
			MergeSort(A,q+1,r);
			Merge(A,p,q,r);
			System.out.println("p:"+p+"  q:"+q);
			SortingUtil.printArray(A, N);
		}
	}
	
	
	public static void Merge(int[] A,int p,int q,int r){
		int n1 = q-p+1;
		int n2 = r-q;
		
		int[] L = new int[n1+2];
		int[] R = new int[n2+2];
		
		for(int i=1;i<=n1;i++)
			L[i] = A[p+i-1];
		
		for(int j=1;j<=n2;j++)
			R[j] = A[q+j];
		
		//哨兵元素,避免每一循环都判断L或R已经为空
		L[n1+1] = Integer.MAX_VALUE;
		R[n2+1] = Integer.MAX_VALUE;
		
		int i = 1,j = 1;
		for(int k=p;k<=r;k++){
			if(L[i] <= R[j]){
				A[k] = L[i];
				i++;
			}
			else{
				A[k] = R[j];
				j++;
			}
		}
		
	}
	
	public static void main(String args[]){
		int[] A = {-1,2,4,5,7,1,2,3,6};
		N = A.length-1;
		MergeSort(A,1,A.length-1);
	}
}
```