---
title: 堆排序
categories: 算法
---

本文展示堆排序的代码。

<!--more-->

---

```bash
/**
 * 	【特点】：
 * 		时间复杂度：
 * 			平均情况 : O(n*logn)
 *  		最好情况 : O(n*logn)
 *  		最坏情况 : O(n*logn)
 *  	空间复杂度 ：
 *  		O(1)
 *  	稳定性     :  不稳定
 *
 */

public class HeapSort {
	
	private static int heapSize = 0;

	/**
	 * 	输入 : 数组 A和下标i。
	 * 	功能 :
	 * 		调用  MaxHeapify 时， 假设以 Left(i) , right(i) 为根的2棵二叉树都是最大堆。
	 * 		只是 A[i] 可能违反最大堆性质。
	 * 		如果违反，就将 A[i] “下降” , 使以 A[i] 为根节点的子树成为最大堆。
	 * @param A
	 * @param i
	 */
	public static void MaxHeapify(int[] A,int i){
		int l = i*2;
		int r = i*2+1;
		int largest = -1;
		if(l<=heapSize && A[l]>A[i])
			largest = l;
		else
			largest = i;
		
		if(r<=heapSize && A[r]>A[largest])
			largest = r;
		
		if(largest!=i){
			SortingUtil.swap(A, largest, i);
			MaxHeapify(A,largest);
		}
	}
	
	/**
	 *  调整最大堆的不同的写法,使用循环而不是递归。
	 */
	public static void MaxHeapify_loop(int[] A,int i){
		int l ,r , largest;
		while(2*i<=heapSize){
			l = 2*i;
			if(l<=heapSize && A[l]>A[i])
				largest = l;
			else
				largest = i;
			
			r = 2*i+1;
			if(r<=heapSize && A[r]>A[largest])
				largest = r;
			
			if(largest!=i){
				SortingUtil.swap(A, largest, i);
				i = largest;
			}
			else
				break;
		}
	}
	
	
	/*
	 * 这是另一种写法， 主要是减少了元素交换的次数。
	 */
	public static void MaxHeapify_loop2(int[] A,int i){
		
		int tmp = A[i] ; //保存当前 A[i]的值
		int child = i*2;	//左孩子的下标
		while(child<=heapSize){	//如果左孩子存在
			
			//如果右孩子存在并且 右孩子的值大于左孩子的，child下标指向右孩子。
			if(child+1<=heapSize && A[child]<A[child+1])
				child = child+1;
			
			if(tmp >= A[child])
				break;
			
			// 把孩子结点的值赋给父结点
			A[i] = A[child];
			
			//选取孩子结点的左孩子结点,继续向下筛选
			i = child;
			child = i*2;
		}
		A[i] = tmp;
	}
	
	public static void BuildMaxHeap(int[] A){
		for(int i = heapSize/2;i>=1;i--){
			MaxHeapify_loop(A,i);
		}
	}
	
	public static void heapSort(int[] A){
		BuildMaxHeap(A);
		for(int i=heapSize;i>=2;i--){
			SortingUtil.swap(A, 1, i);
			System.out.print(A[i]+" ");
			heapSize--;
			MaxHeapify_loop2(A,1);
		}
	}
	
	public static void main(String[] args){
		int[] A = {-1,4,1,3,2,16,9,10,14,8,7};
		heapSize = A.length-1;
		heapSort(A);
		SortingUtil.printArray(A, heapSize);
	}
}
```