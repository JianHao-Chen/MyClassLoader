---
title: 冒泡排序
categories: 算法
---

本文展示冒泡排序的代码。

<!--more-->

---

```bash
/**
 * 	思路:
 * 		重复地走访过要排序的数列，一次比较两个元素，如果他们的顺序错误就把他们交换过来。
 * 		走访数列的工作是重复地进行直到没有再需要交换，也就是说该数列已经排序完成。
 * 
 * 	【特点】：
 * 		时间复杂度：
 * 			平均情况 : O(n*n)
 *  		最好情况 : O(n)
 *  		最坏情况 : O(n*n)
 *  	空间复杂度 ：
 *  		O(1)
 *  	稳定性     : 稳定
 *
 */

public class BubbleSort {
	
	public static void BubbleSort(int[] A , int n){
		for(int i=1;i<=n;i++){
			for(int j=n;j>i;j--){
				if(A[j]<A[j-1])
					SortingUtil.swap(A, j, j-1);
			}
			SortingUtil.printArray(A, n);
		}
	}
	
	public static void main(String[] args){
		int[] A = {-1,5,2,4,6,1,3};
		BubbleSort(A,A.length-1);
	}
}
```