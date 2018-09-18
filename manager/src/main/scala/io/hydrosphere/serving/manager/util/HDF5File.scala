package io.hydrosphere.serving.manager.util

import java.io.Closeable

import org.bytedeco.javacpp.{BytePointer, Loader, hdf5}

import scala.collection.mutable

/**
  * Readonly H5 wrapper to read attributes.
  */
class HDF5File(val h5File: hdf5.H5File) extends Closeable {

  override def close(): Unit = {
    h5File.deallocate()
  }

  private def openGroups(groups: Seq[String]): Array[hdf5.Group] = {
    val groupArray: Array[hdf5.Group] = new Array[hdf5.Group](groups.length)
    groupArray(0) = this.h5File.openGroup(groups.head)
    var i: Int = 1
    while (i < groups.length) {
      groupArray(i) = groupArray(i - 1).openGroup(groups(i))
      i += 1
    }
    groupArray
  }

  private def closeGroups(groupArray: Array[hdf5.Group]): Unit = {
    groupArray.foreach(_.deallocate())
  }

  /**
    * Read string attribute from group path.
    *
    * @param attributeName Name of attribute
    * @param groups        Array of zero or more ancestor groups from root to parent.
    * @return HDF5 attribute as String
    */
  def readAttributeAsString(attributeName: String, groups: String*): String = {
    if (groups.isEmpty) {
      val a: hdf5.Attribute = this.h5File.openAttribute(attributeName)
      val s: String = readAttributeAsString(a)
      a.deallocate()
      s
    } else {
      val groupArray: Array[hdf5.Group] = openGroups(groups)
      val a: hdf5.Attribute = groupArray(groups.length - 1).openAttribute(attributeName)
      val s: String = readAttributeAsString(a)
      a.deallocate()
      closeGroups(groupArray)
      s
    }
  }

  /**
    * Check whether group path contains string attribute.
    *
    * @param attributeName Name of attribute
    * @param groups        Array of zero or more ancestor groups from root to parent.
    * @return Boolean indicating whether attribute exists in group path.
    */
  def hasAttribute(attributeName: String, groups: String*): Boolean = {
    if (groups.isEmpty) {
      return this.h5File.attrExists(attributeName)
    }
    val groupArray: Array[hdf5.Group] = openGroups(groups)
    val b: Boolean = groupArray(groupArray.length - 1).attrExists(attributeName)
    closeGroups(groupArray)
    b
  }

  /**
    * Get list of data sets from group path.
    *
    * @param groups Array of zero or more ancestor groups from root to parent.
    * @return List of HDF5 data set names
    */
  def getDataSets(groups: String*): List[String] = {
    if (groups.isEmpty) {
      return getObjects(this.h5File, hdf5.H5O_TYPE_DATASET)
    }
    val groupArray: Array[hdf5.Group] = openGroups(groups)
    val ls: List[String] = getObjects(groupArray(groupArray.length - 1), hdf5.H5O_TYPE_DATASET)
    closeGroups(groupArray)
    ls
  }

  /**
    * Get list of groups from group path.
    *
    * @param groups Array of zero or more ancestor groups from root to parent.
    * @return List of HDF5 groups
    */
  def getGroups(groups: String*): List[String] = {
    if (groups.isEmpty) {
      return getObjects(this.h5File, hdf5.H5O_TYPE_GROUP)
    }
    val groupArray: Array[hdf5.Group] = openGroups(groups)
    val ls: List[String] = getObjects(groupArray(groupArray.length - 1), hdf5.H5O_TYPE_GROUP)
    closeGroups(groupArray)
    ls
  }

  /**
    * Get list of objects with a given type from a file group.
    *
    * @param fileGroup HDF5 file or group
    * @param objType   Type of object as integer
    * @return List of HDF5 group objects
    */
  private def getObjects(fileGroup: hdf5.Group, objType: Int): List[String] = {
    val groups = mutable.ListBuffer.empty[String]
    var i: Int = 0
    while (i < fileGroup.getNumObjs) {
      val objPtr: BytePointer = fileGroup.getObjnameByIdx(i)
      if (fileGroup.childObjType(objPtr) == objType) {
        groups += fileGroup.getObjnameByIdx(i).getString
      }
      i += 1
    }
    groups.toList
  }

  /**
    * Read attribute as string.
    *
    * @param attribute HDF5 attribute to read as string.
    * @return HDF5 attribute as string
    */
  private def readAttributeAsString(attribute: hdf5.Attribute): String = {
    val vl: hdf5.VarLenType = attribute.getVarLenType
    var bufferSizeMult: Int = 1
    var s = ""
    var flag = true
    while (flag) {
      val attrBuffer: Array[Byte] = new Array[Byte](bufferSizeMult * 2000)
      val attrPointer: BytePointer = new BytePointer(attrBuffer: _*)
      attribute.read(vl, attrPointer)
      attrPointer.get(attrBuffer)
      s = new String(attrBuffer)
      if (s.endsWith("\u0000")) {
        s = s.replace("\u0000", "")
        flag = false
      } else {
        bufferSizeMult += 1
        if (bufferSizeMult > 1000) {
          throw new IllegalArgumentException("Could not read abnormally long HDF5 attribute")
        }
      }
    }
    vl.deallocate()
    s
  }

}

object HDF5File {
  try {
    /* This is necessary for the call to the BytePointer constructor below. */
    Loader.load(classOf[hdf5])
  } catch {
    case e: Exception =>
      e.printStackTrace()
  }

  def apply(h5Filename: String) = {
    new HDF5File(new hdf5.H5File(h5Filename, hdf5.H5F_ACC_RDONLY))
  }
}