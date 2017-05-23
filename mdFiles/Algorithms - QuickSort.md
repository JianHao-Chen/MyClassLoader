---
title: 快速排序
categories: 算法
---

本文展示插入排序的代码。
<!--more-->

---

```bash
public class QuickSort {
	
	
	public static int len;

	/**	【分治】 :
	 * 		分解 : 
	 * 			数组 A[p..r]被划分为2个(可能空)子数组  A[p..q-1]和A[q+1..r],
	 * 			使A[p..q-1]中的每个元素都<=A[q],而且，<= A[q+1..r]中的元素。
	 * 		解决 :
	 * 			通过递归调用快速排序,对子数组A[p..q-1]和A[q+1..r]排序
	 * 		合并 :
	 * 			子数组是就地排序的，合并不需要操作。
	 * 
	 * @param A
	 * @param p
	 * @param r
	 */
	public static void quickSort(int[] A, int p, int r){
		if(p<r){
			int q = partition(A,p,r);
			quickSort(A,p,q-1);
			quickSort(A,q+1,r);
		}
	}
	
	
	/**
	 * 从前到后遍历数组
	 * @param A
	 * @param p
	 * @param r
	 * @return
	 */
	public static int partition(int[] A, int p, int r){
		int tmp = A[r];
		int i = p-1;
		for(int j = p;j<=r-1;j++){
			if(A[j]<=tmp){
				i++;
				SortingUtil.swap(A, i, j);
			}
		}
		SortingUtil.swap(A, i+1, r);
		SortingUtil.printArray(A, len);
		return i+1;
	}
	
	/*
	 * 	1．i =L; j = R; 将基准数挖出形成第一个坑a[i]。
	 * 	2．j--由后向前找比它小的数，找到后挖出此数填前一个坑a[i]中。
	 * 	3．i++由前向后找比它大的数，找到后也挖出此数填到前一个坑a[j]中。
	 * 	4．再重复执行2，3二步，直到i==j，将基准数填入a[i]中。
	 */
	public static int partition2(int[] A,int p, int r){
		int i = p, j = r;
		int x = A[p];	//x保存第一个元素
		while(i<j){
			//从右向左找小于x的数来填A[i]
			while(i < j && A[j] >= x)   
	            j--;
			
			if(i<j){
				A[i] = A[j];
				i++;	//交换后i指针+1
			}
			
			//从左向右找大于或等于x的数来填A[j]
			while(i < j && A[i] < x)  
	            i++;
			
			if(i<j){
				//将s[i]填到s[j]中，s[i]就形成了一个新的坑  
				A[j] = A[i]; 
	            j--; 	//交换后j指针-1
			}
			
		}
		//退出时，i等于j。将x填到这个坑中。  
	    A[i] = x;
	    return i; 
	}
	
	
	public static void main(String[] args){
		int[] A = {-1,2,8,7,1,3,5,6,4};
		len = A.length-1;
		quickSort(A,1,len);
		
	}
}
```