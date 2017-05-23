---
title: 二叉树的相关操作
categories: 算法
---

本文展示二叉树的各种遍历、插入、删除的操作。
<!--more-->

---

```bash
class BinTreeNode{
    int val;
    BinTreeNode left;
    BinTreeNode right;
    BinTreeNode parent;
    
    public String toString(){
        return this.val+"";
    }
}

public class BinaryTree {
    
    public static void inOrderTraverse(BinTreeNode T){
        if(T==null) return;
        inOrderTraverse(T.left);
        System.out.print(T.val+" ");
        inOrderTraverse(T.right);
    }
    
    public static void levelTraverse(BinTreeNode T){
        Queue<BinTreeNode> queue = new LinkedList<BinTreeNode>();
        queue.offer(T);
        
        int nodesInCurrentLevel = 1;  
        int nodesInNextLevel = 0; 
        
        while(!queue.isEmpty()){
            BinTreeNode p = queue.poll();
            nodesInCurrentLevel--;
            if(p.left!=null) {
                queue.offer(p.left);
                nodesInNextLevel++;
            }
            if(p.right!=null) {
                queue.offer(p.right);
                nodesInNextLevel++;
            }
            System.out.print(p+" ");
            
            if(nodesInCurrentLevel==0){
                nodesInCurrentLevel = nodesInNextLevel;
                nodesInNextLevel = 0;
                System.out.println("");
            }
            
        }
    }
    
    public static BinTreeNode insert(BinTreeNode T,int x){
        if(T==null){
            BinTreeNode node = new BinTreeNode();
            node.left = null;
            node.right = null;
            node.val = x;
            T = node;
            return T;
        }
        if(T.val==x) return T;
        else if(T.val<x){
            T.right = insert(T.right,x);
            T.right.parent = T;
        }
        else{
            T.left = insert(T.left,x);
            T.left.parent = T;
        }
        return T;
    }

//--------------------------------------------------------------------------    
    /*
     * 非递归遍历
     * 
     *              4
     *            /   \
     *           3     8
     *          /     / \
     *         2     7   10
     */
    
    //  先序遍历    (this -> left -> right)
    public static void preOrderNotRecursive(BinTreeNode T){
        if(T==null) return;
        Stack<BinTreeNode> stack = new Stack<BinTreeNode>();
        BinTreeNode p = T;
        while(!stack.isEmpty() || p!=null){
            if(p!=null){
                System.out.print(p+" ");
                stack.push(p);
                p = p.left;
            }else{
                p = stack.pop();
                p = p.right;
            }
        }
    }
    
    //  中序遍历    (left -> this -> right)
    public static void inOrderNotRecursive(BinTreeNode T){
        if(T==null) return;
        Stack<BinTreeNode> stack = new Stack<BinTreeNode>();
        BinTreeNode p = T;
        while(!stack.isEmpty() || p!=null){
            if(p!=null){
                stack.push(p);
                p = p.left;
            }else{
                p = stack.pop();
                System.out.print(p+" ");
                p = p.right;
            }
        }
    }
    
    /**
     *  非递归 中序遍历  , 非递归 & 不用栈（因为BinTreeNode里面保存了parent引用）
     */
    public static void inOrderNotRecursive_NoStack(BinTreeNode T){
        if(T==null) return;
        BinTreeNode p = T;
        
        //先找出最小的元素
        while(p.left!=null){
            p = p.left;
        }
        
        while(p!=T){
            while(p.left!=null){
                p = p.left;
            }
            // 访问当前节点
            System.out.print(p+" ");
            // 如果有右节点,转向右子树
            if(p.right!=null){
                p = p.right;
            }
            else{   // 向上返回
                while(p.parent!=null){
                    // 如果是左子树返回,返回到它的父节点
                    if(p==p.parent.left){
                        System.out.print(p.parent.val+" ");
                        // 如果有右节点,转向右子树
                        if(p.parent.right!=null){
                            p = p.parent.right;
                            break;
                        }
                        else
                            p = p.parent;
                    }
                    else
                        p = p.parent;
                }
            }
        }
    }
    
    
    //  后序遍历 （非递归）    (left -> right -> this)
    public static void postOrderNotRecursive(BinTreeNode T){
        if(T==null) return;
        Stack<BinTreeNode> stack = new Stack<BinTreeNode>();
        BinTreeNode p = T;
        
        /**
         *  pre指向p之前访问的节点,如果 pre==p.right
         *  表示当前节点是从右孩子返回的。
         */
        BinTreeNode pre = null;
        while(!stack.isEmpty() || p!=null){
            if(p!=null){
                stack.push(p);
                pre = p;
                p = p.left;
            }else{
                p = stack.peek();
                if(p.right==null || pre == p.right){
                    System.out.print(p+" ");
                    pre = p;
                    p = null;
                    stack.pop();
                }
                else{
                    p = p.right;
                }
            }
        }
    }
    
//--------------------------------------------------------------------------
    
    /**
     *  查找 T节点的后继节点
     */
    public static BinTreeNode successor(BinTreeNode T){
        
        if(T==null)return null;
        BinTreeNode p = null,q = null;
        
        if(T.right!=null){
            p = T.right;
            while(p.left!=null)
                p = p.left;
            return p;
        }
        else{
            p = T;
            q = p.parent;
            while(q!=null && q.right==p){
                p = q;
                q = q.parent;
            }
            return q;
        }
    }
    
    /**
     *  查找 T节点的前驱节点
     */
    public static BinTreeNode predecessor(BinTreeNode T){
        if(T==null)return null;
        BinTreeNode p = null,q = null;
        
        if(T.left!=null){
            p = T.left;
            while(p.right!=null)
                p = p.right;
            return p;
        }
        else{
            p = T;
            q = p.parent;
            while(q!=null && q.left==p){
                p = q;
                q = q.parent;
            }
            return q;
        }
    }
    
    /**
     * 在根节点T下查找值为x的节点
     * @param T
     * @param x
     * @return
     */
    public static BinTreeNode search(BinTreeNode T,int x){
        BinTreeNode p = T;
        while(p!=null && p.val!=x){
            if(p.val<x) p = p.right;
            else    p = p.left;
        }
        return p;
    }
    
    /*
     * 在根节点T下删除x节点
     */
    public static BinTreeNode Delete(BinTreeNode T,BinTreeNode x){
        
        BinTreeNode y = null;
        if(x.left==null && x.right==null){
            y = x.parent;
            if(x==y.left)
                y.left = null;
            else
                y.right = null;
        }
        else if(x.left==null || x.right==null){
            if(x.left!=null)
                y = x.left;
            else
                y = x.right;
            // 可以直接修改指针完成删除操作
            if(x==x.parent.left){
                x.parent.left = y;
                y.parent = x.parent;
            }
            else{
                x.parent.right = y;
                y.parent = x.parent;
            }
        }else{
            
            /*
             *  y 一定没有左孩子,因为y是x的直接后继。
             *  假设y有左孩子 z , 那么 执行 successor(x)应该会得到 z而不会是 y!
             */
            y = successor(x);
            
            // 于是就可以转化为删除 y 的操作 ^_^
            
            BinTreeNode tmp = null;
            if(y.right!=null){
                tmp = y.right;
                tmp.parent = y.parent;
                if(y.parent.left==y)
                    y.parent.left = tmp;
                else
                    y.parent.right = tmp;
            }
            else{
                if(y.parent.left==y)
                    y.parent.left = null;
                else
                    y.parent.right = null;
            }
            x.val = y.val;
        }
        return T;
    }
    
    
    
    public static void main(String[] args) {
        BinTreeNode T = null;
        int[] a = {-1,4,3,9,2,7,8,10};
        for(int i=1;i<=7;i++)
            T = insert(T,a[i]);
        
        BinTreeNode node7 = search(T,7);
        System.out.println(node7);
        BinTreeNode Prenode = predecessor(node7);
        System.out.println(Prenode);
        
        levelTraverse(T);
        T = Delete(T,Prenode);
        levelTraverse(T);
        
 //       postOrderNotRecursive(T);
 //     levelTraverse(T);
    }
}
```