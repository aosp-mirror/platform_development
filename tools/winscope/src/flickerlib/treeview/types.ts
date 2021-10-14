import Chip from './Chip'

export type TreeViewObject = {
  kind: String
  name: String
  shortName: String
  stableId: String | Number
  chips: Chip[]
  obj: any
  children: TreeViewObject[]
  ref: any
}