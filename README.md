# The Gumtree dataset of diffs

This repository contains the diff files used in the paper "Fine-grained and Accurate Source Code Differencing".

Folder data1 contains the 144 diffs used in the manual analysis of Section 5.1.

For a given pair of files, it gives:

* the original files before diff ([before version](https://github.com/monperrus/dataset-diff-gumtree/blob/master/data1/t_202564/left_PropPanelModelElement_1.9.java) and [after version](https://github.com/monperrus/dataset-diff-gumtree/blob/master/data1/t_202564/left_PropPanelModelElement_1.9.java)) 
* the [unified diff](https://github.com/monperrus/dataset-diff-gumtree/blob/master/data1/t_202564/t_202564-diff-unified.diff)
* the [patience diff](https://github.com/monperrus/dataset-diff-gumtree/blob/master/data1/t_202564/t_202564-git-patience.diff) (as implemented in git)
* the [gumtree diff](https://github.com/monperrus/dataset-diff-gumtree/blob/master/data1/t_202564/t_202564-git-patience.diff)


